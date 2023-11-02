package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Autowired
    private ShopMapper shopMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result querybyid(Long id) {
        //缓存穿透解决方案↓
        //Shop shop = querybyid_huancunchuantou(id);
        //缓存击穿↓
        //Shop shop = querybyid_huancunchuantou(id);
        //逻辑过期解决方案
        Shop shop = querybyid_huancunchuantou_luojiguoqi(id);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    private Shop querybyid_huancunchuantou(Long id) {
        //从redis查询
        String s = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        if (StrUtil.isNotBlank(s)) {
//对象是存在
            //有数据 直接返回

            return JSONUtil.toBean(s, Shop.class);
        }
        //判断命中的是否是空值
        if (s != null) {
            log.info("缓存了空对象");
            return null;
        }
        //从数据库查询
        //不存在
        Shop byId = this.getById(id);
        if (BeanUtil.isEmpty(byId)) {
            //空值写去redis
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //存在 存入redis
        String jsonStr = JSONUtil.toJsonStr(byId);
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, jsonStr, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return byId;
    }

    private Shop querybyid_huancunjichuan(Long id) {
        //从redis查询
        String s = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        if (StrUtil.isNotBlank(s)) {
//对象是存在
            //有数据 直接返回
            return JSONUtil.toBean(s, Shop.class);
        }
        //判断命中的是否是空值
        if (s != null) {
            log.info("缓存了空对象");
            return null;
        }
        //从数据库查询
        //缓存重建
        //获取互斥锁
        String lockKey = "lock:login:" + id;
        Shop byId = null;
        try {
            boolean tolock = tolock(lockKey);
            if (!tolock) {
                Thread.sleep(100);
                //递归
                return querybyid_huancunchuantou(id);
            }
            byId = this.getById(id);
            if (BeanUtil.isEmpty(byId)) {
                //空值写去redis
                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            //存在 存入redis
            String jsonStr = JSONUtil.toJsonStr(byId);
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, jsonStr, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unlock(lockKey);
        }

        return byId;


    }

    @Override
    @Transactional
    public Result updateshop(Shop shop) {
        this.updateById(shop);
        String key = CACHE_SHOP_KEY + shop.getId();
        if (StrUtil.isEmpty(key)) {
            return Result.fail("查询失败，电批id为空");
        }
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return Result.ok(shop);
    }


    private boolean tolock(String key) {
        Boolean b = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        //不要直接返回 可能会拆箱空指针
        return BooleanUtil.isTrue(b);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    public void saveShop2Redis(Long id, Long time) throws InterruptedException {
        Shop byId = this.getById(id);
        RedisData redisData = new RedisData();
        redisData.setData(byId);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(time));
        Thread.sleep(200);
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }

    //定义线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    //采用逻辑过期时间
    private Shop querybyid_huancunchuantou_luojiguoqi(Long id) {
        //从redis查询
        String s = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        if (StrUtil.isBlank(s)) {

            return null;
        }

        RedisData bean = JSONUtil.toBean(s, RedisData.class);
        Object data = bean.getData();
        Shop getshop =JSONUtil.toBean(JSONUtil.toJsonStr(data),Shop.class);
        LocalDateTime expireTime = bean.getExpireTime();
        log.info("取出了" + getshop + "过期时间" + expireTime);
        //判断过期时间
        if (expireTime.isAfter(LocalDateTime.now())) {
            //未过期
            log.info("未过期");
            return getshop;
        }
        //重建缓存
        String lockkey = LOCK_SHOP_KEY + s;
        boolean tolock = tolock(lockkey);
        if (BooleanUtil.isTrue(tolock)) {
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    this.saveShop2Redis(id, 20L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unlock(lockkey);
                }
            });
        }
        return getshop;
    }
}

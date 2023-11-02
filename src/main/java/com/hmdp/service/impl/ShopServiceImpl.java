package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
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
        //从redis查询
        String s = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY+id);
        if (StrUtil.isNotBlank(s)){
//对象是存在
            //有数据 直接返回
            Shop shop = JSONUtil.toBean(s, Shop.class);
            return Result.ok(shop);
        }
        //判断命中的是否是空值
        if (s != null){
            log.info("缓存了空对象");
            return Result.fail("店铺不存在");
        }

        //从数据库查询
        //不存在
        Shop byId = this.getById(id);
        if (BeanUtil.isEmpty(byId)){
            //空值写去redis
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id, "",CACHE_NULL_TTL, TimeUnit.MINUTES);
            return Result.fail("店铺不存在");
        }

        //存在 存入redis
        String jsonStr = JSONUtil.toJsonStr(byId);
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id, jsonStr,CACHE_SHOP_TTL, TimeUnit.MINUTES);

        return Result.ok(byId);
    }

    @Override
    @Transactional
    public Result updateshop(Shop shop) {
        this.updateById(shop);
        String key = CACHE_SHOP_KEY + shop.getId();
        if (StrUtil.isEmpty(key)){
            return Result.fail("查询失败，电批id为空");
        }
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL,TimeUnit.MINUTES);
        return Result.ok(shop);
    }
}

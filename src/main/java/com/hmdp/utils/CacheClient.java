package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.security.KeyPair;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

/****************************
 * @project hm-dianping
 * @package com.hmdp.utils
 * @className CacheClient
 * @author Zjiah
 * @date 2023/11/2 15:17
 * @Description:   *
 ****************************/
@Component
@Slf4j
public class CacheClient {
    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    //定义线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    private final StringRedisTemplate stringRedisTemplate;

    public void set(String key, Object object, Long time, TimeUnit timeUnit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(object), time, timeUnit);
    }

    private boolean tolock(String key) {
        Boolean b = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        //不要直接返回 可能会拆箱空指针
        return BooleanUtil.isTrue(b);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }


    /**
     * @param key
     * @param object
     * @param time
     * @param timeUnit
     * 写去redis时候进行设置过期时间
     */
    public void setWithLocaldataPlus(String key, Object object, Long time, TimeUnit timeUnit) {
        RedisData redisData = new RedisData();
        redisData.setData(object);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(time)));

        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * @param keyPrefix
     * @param id
     * @param type
     * @param dbFallback
     * @param time
     * @param timeUnit
     * @return {@code R}
     * 缓存穿透————————————————————————————————————
     */
    public <R, ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit timeUnit) {
        //从redis查询
        String s = stringRedisTemplate.opsForValue().get(keyPrefix + id);
        if (StrUtil.isNotBlank(s)) {
//对象是存在
            //有数据 直接返回
            return JSONUtil.toBean(s, type);
        }
        //判断命中的是否是空值
        if (s != null) {
            log.info("缓存了空对象");
            return null;
        }
        //从数据库查询
        //不存在
        R sel = dbFallback.apply(id);
        if (BeanUtil.isEmpty(sel)) {
            //空值写去redis
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //存在 存入redis
        this.set(keyPrefix + id, sel, time, timeUnit);
        return sel;
    }


    /**
     * @param keyPrefix
     * @param id
     * @param type
     * @param function
     * @param time
     * @param timeUnit
     * @return {@code R}
     * 缓存击穿————互斥锁————返回空值的——————————————————————————
     */
    public <R, ID> R queryWithMutex(String keyPrefix, ID id, Class<R> type, Function<ID, R> function, Long time, TimeUnit timeUnit) {
        //从redis查询
        String s = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        if (StrUtil.isNotBlank(s)) {
            //对象是存在
            //有数据 直接返回
            return JSONUtil.toBean(s, type);
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
        R r = null;
        try {
            boolean tolock = tolock(lockKey);
            if (!tolock) {
                Thread.sleep(100);
                //递归
                return this.queryWithMutex(keyPrefix,id,type,function,time,timeUnit);
            }
            r = function.apply(id);
            if (BeanUtil.isEmpty(r)) {
                //空值写去redis
                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            //存在 存入redis
            String jsonStr = JSONUtil.toJsonStr(r);
            this.set(keyPrefix + id, r, time, timeUnit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unlock(lockKey);
        }

        return r;


    }


    /**
     * @param keyPrefix
     * @param id
     * @param type
     * @param function
     * @param time
     * @param timeUnit
     * @return {@code R}
     * 缓存击穿————采用逻辑过期时间的——————————————————————
     */
    public <R, ID> R  queryWithLogicalExpire(String keyPrefix, ID id, Class<R> type, Function<ID, R> function, Long time, TimeUnit timeUnit) {
        //从redis查询
        String s = stringRedisTemplate.opsForValue().get(keyPrefix + id);
        if (StrUtil.isBlank(s)) {
            return null;
        }

        RedisData bean = JSONUtil.toBean(s, RedisData.class);
        Object data = bean.getData();
        R getObject = JSONUtil.toBean(JSONUtil.toJsonStr(data),type);
        LocalDateTime expireTime = bean.getExpireTime();
        log.info("取出了" + getObject + "过期时间" + expireTime);
        //判断过期时间
        if (expireTime.isAfter(LocalDateTime.now())) {
            //未过期
            log.info("未过期");
            return getObject;
        }
        //重建缓存
        String lockkey = keyPrefix + id;
        boolean tolock = tolock(lockkey);
        if (BooleanUtil.isTrue(tolock)) {
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    R r = function.apply(id);
                    //写入
                    this.setWithLocaldataPlus(keyPrefix + id,r,time,timeUnit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unlock(lockkey);
                }
            });
        }
        return getObject;
    }



}

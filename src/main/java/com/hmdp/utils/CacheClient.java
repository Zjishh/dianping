package com.hmdp.utils;

import cn.hutool.json.JSONUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/****************************
 * @project hm-dianping
 * @package com.hmdp.utils
 * @className CacheClient
 * @author Zjiah
 * @date 2023/11/2 15:17
 * @Description:   *
 ****************************/
@Component
public class CacheClient {
    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private final StringRedisTemplate stringRedisTemplate;

    public void set(String key, Object object, Long time, TimeUnit timeUnit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(object),time,timeUnit);
    }

}

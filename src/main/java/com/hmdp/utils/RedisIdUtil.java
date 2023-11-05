package com.hmdp.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/****************************
 * @project hm-dianping
 * @package com.hmdp.utils
 * @className redisIdUtil
 * @author Zjiah
 * @date 2023/11/3 19:59
 * @Description:   *
 ****************************/
@Slf4j
@Component
public class RedisIdUtil {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final long BEGIN_TIMESTAMP = 1000512000L;
    private static final int WEIYUNSUAN = 32;

    public Long nexiId(String keyPrefix){
        //时间戳
        LocalDateTime localDateTime = LocalDateTime.now();
        long now = localDateTime.toEpochSecond(ZoneOffset.UTC);
        long nowStamp = now - BEGIN_TIMESTAMP;

        String localDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));

        //序列号
        Long increment = stringRedisTemplate.opsForValue().increment("incrementID:" + keyPrefix + ":" + localDate + ":"+nowStamp);


        return nowStamp << WEIYUNSUAN | increment;
    }




}

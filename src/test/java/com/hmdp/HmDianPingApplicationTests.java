package com.hmdp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.RedisIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@SpringBootTest
class HmDianPingApplicationTests {
    @Autowired
    private RedisIdUtil redisIdUtil;

    private ExecutorService executorService = Executors.newFixedThreadPool(500);


    @Autowired
    private ShopMapper shopMapper;
    @Autowired
    private ShopServiceImpl shopService;

    @Test
        //导入redis
    void testShop() throws InterruptedException {
        List<Shop> shopids = shopMapper.selectAllId();
        for (Shop shopid : shopids) {
            Long id = shopid.getId();
            shopService.saveShop2Redis(id, 200L);
        }

    }

    @Test
    void restID() {


        log.info("开始");
        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long order = redisIdUtil.nexiId("order");
                log.info("id={}", order);
            }
        };

        for (int i = 0; i < 100; i++) {
            executorService.submit(task);
        }


    }

}

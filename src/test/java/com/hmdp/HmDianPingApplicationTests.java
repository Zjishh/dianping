package com.hmdp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.impl.ShopServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class HmDianPingApplicationTests {

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
            shopService.saveShop2Redis(id,200L);
        }

    }

}

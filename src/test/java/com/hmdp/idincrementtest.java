package com.hmdp;

import com.hmdp.utils.RedisIdUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/****************************
 * @project hm-dianping
 * @package com.hmdp
 * @className idincrementtest
 * @author Zjiah
 * @date 2023/11/3 20:25
 * @Description:   *
 ****************************/
@SpringBootTest
public class idincrementtest {

    @Autowired
    private RedisIdUtil redisIdUtil;

    @Test
    public void t(){
        Long order = redisIdUtil.nexiId("order");
        System.out.println("id= "+order);
    }


}

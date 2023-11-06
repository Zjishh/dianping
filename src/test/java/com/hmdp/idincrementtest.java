package com.hmdp;

import com.hmdp.utils.RedisIdUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
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

    //线程池
    private ExecutorService es = Executors.newFixedThreadPool(500);

    @Autowired
    private RedisIdUtil redisIdUtil;

    @Test
    public void t() throws InterruptedException {
        CountDownLatch lLatch = new CountDownLatch(300);

        Runnable task = () ->{
            for (int i = 0; i < 100; i++) {
                long order = redisIdUtil.nexiId("order");
                System.out.println("id = " + order);
            }
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        lLatch.await();
        long end = System.currentTimeMillis();
        System.out.println("times = "+ (end-begin));
    }


}

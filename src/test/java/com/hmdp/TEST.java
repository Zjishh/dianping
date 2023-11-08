package com.hmdp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/****************************
 * @project hm-dianping
 * @package com.hmdp
 * @className TEST
 * @author Zjiah
 * @date 2023/11/3 13:50
 * @Description:   *
 ****************************/
@SpringBootTest
public class TEST {
    @Test
    public static void main(String[] args) {
        MyThread myThread = new MyThread();
        MyThread myThread2 = new MyThread();

        myThread2.setName("222");
        myThread.setName("111");

        myThread2.start();
        myThread.start();
    }

     public static class MyThread extends Thread{

         @Override
         public void run() {
             for (int i = 0; i < 100; i++) {
                 try {
                     Thread.sleep(200L);
                 } catch (InterruptedException e) {
                     throw new RuntimeException(e);
                 }
                 System.out.println(getName()+"--------");
             }

         }
     }
}

package com.hmdp;

import cn.hutool.core.lang.intern.InternUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/****************************
 * @project hm-dianping
 * @package com.hmdp
 * @className testt
 * @author Zjiah
 * @date 2023/11/3 17:53
 * @Description:   *
 ****************************/
@SpringBootTest
public class testt {

    public static void main(String[] args) {
        int x = 10;
        System.out.println(isPalindrome(x));
    }

    @Test
    public static boolean isPalindrome(int x) {
        if (x < 0)
            return false;
        String i1 = String.valueOf(x);
        int[] xs = new int[i1.length()];
        for (int i = 0; i < i1.length(); i++) {
            // 遍历str将每一位数字添加如intArray
            Character ch = i1.charAt(i);
            xs[i] = Integer.parseInt(ch.toString());
        }



        for (int i = 0; i < xs.length; i++) {
            if (xs[i] != xs[xs.length - 1 - i]) {
                return false;
            }
        }
        return true;

    }
}

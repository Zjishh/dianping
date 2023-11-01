package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result querybyid(Long id) {
        //从redis查询
        String s = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY+id);
        if (StrUtil.isNotBlank(s)){
            //有数据 直接返回
            Shop shop = JSONUtil.toBean(s, Shop.class);
            return Result.ok(shop);
        }
        //从数据库查询
        Shop byId = this.getById(id);
        if (BeanUtil.isEmpty(byId)){
            return Result.fail("店铺不存在");
        }

        //存在 存入redis
        String jsonStr = JSONUtil.toJsonStr(byId);
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id, jsonStr,CACHE_SHOP_TTL, TimeUnit.MINUTES);

        return Result.ok(byId);
    }
}

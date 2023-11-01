package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private UserMapper userMapper;

    @Override
    public Result sendCode(String phone, HttpSession session) {

        if (RegexUtils.isPhoneInvalid(phone))
            return Result.fail("手机号格式不对");

//        String s = RandomUtil.randomNumbers(6);
        String s = "111111";

        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, s, LOGIN_CODE_TTL, TimeUnit.MINUTES);
//        session.setAttribute(phone, s);

        log.info("发送验证码案成功---》{}", s);

        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {


        String inphone = loginForm.getPhone();
        String incode = loginForm.getCode();

        String code = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY+inphone);

        if (RegexUtils.isPhoneInvalid(inphone))
            return Result.fail("手机号格式不对");

        if ((incode == null) || (!incode.equals(code)))
            return Result.fail("验证码不正确");

        User user = userMapper.selectbyphone(inphone);

        if (user == null) {
            String token =  UUID.randomUUID().toString(true);
            String username = USER_NICK_NAME_PREFIX + RandomUtil.randomString(10);
            userMapper.loginone(inphone, username);
            LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
            q.eq(User::getPhone, inphone);
            User user_read = userMapper.selectOne(q);
            UserDTO userDTO_read = BeanUtil.copyProperties(user_read, UserDTO.class);
//            session.setAttribute("user",BeanUtil.copyProperties(user_read,UserDTO.class));
            Map<String, Object> userDto_read_map = BeanUtil.beanToMap(userDTO_read,new HashMap<>(),CopyOptions.create()
                    .setIgnoreNullValue(true)
                    .setFieldValueEditor((fieldNmae,fieldValue)->fieldValue.toString()));
            String tokens = LOGIN_USER_KEY + token;
            stringRedisTemplate.opsForHash().putAll(tokens, userDto_read_map);
            stringRedisTemplate.expire(tokens, LOGIN_USER_TTL, TimeUnit.MINUTES);
            return Result.ok();
        } else {
            UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
            Map<String, Object> userDto_map = BeanUtil.beanToMap(userDTO,new HashMap<>(),CopyOptions.create()
                    .setIgnoreNullValue(true)
                    .setFieldValueEditor((fieldNmae,fieldValue)->fieldValue.toString()));
            String token = UUID.randomUUID().toString(true);
            String tokens = LOGIN_USER_KEY + token;
            stringRedisTemplate.opsForHash().putAll(tokens, userDto_map);
            stringRedisTemplate.expire(tokens, LOGIN_USER_TTL, TimeUnit.MINUTES);
            return Result.ok(token);
        }

    }

    @Override
    public Result me() {
        return null;
    }
}

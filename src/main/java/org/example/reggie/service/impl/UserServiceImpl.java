package org.example.reggie.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.reggie.common.R;
import org.example.reggie.entity.User;
import org.example.reggie.mapper.UserMapper;
import org.example.reggie.service.UserService;
import org.example.reggie.utils.RegexUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.example.reggie.common.RedisConstants.LOGIN_CODE_KEY;
import static org.example.reggie.common.RedisConstants.LOGIN_CODE_TTL;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public R<String> sendCode(String phone) {
        // 1.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2.如果手机号不符合，返回错误信息
            return R.error("手机号格式错误!");
        }

        // 3.符合，生成验证码
        String code = RandomUtil.randomNumbers(6);

        // session.setAttribute(msgCode, code);// 4.保存验证码到session
        // 4.保存验证码到redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

        // 5.发送验证码
        log.info("发送验证码成功：验证码：{}", code);

        // 返回ok
        return R.success("发送验证码成功，五分钟内有效！");
    }

    @Override
    public R<User> login(Map map, HttpSession session) {
        String phone = map.get("phone").toString();
        // 1.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2.如果手机号不符合，返回错误信息
            return R.error("手机号格式错误");
        }

        // 3.从redis中获取验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = map.get("code").toString();
        if (cacheCode == null || !cacheCode.toString().equals(code)) {
            return R.error("验证码错误！");
        }

        // 4.验证码一致
        User user = query().eq("phone", phone).one();

        // 5.判断用户是否存在
        if (user == null) {
            // 6.不存在，创建新用户并保存
            user = createUserWithPhone(phone);
        }
        session.setAttribute("user", user.getId());

        // 如果用户登录成功，删除redis中缓存的验证码
        stringRedisTemplate.delete(LOGIN_CODE_KEY + phone);

        // 返回
        return R.success(user);
    }

    private User createUserWithPhone(String phone) {
        // 1.创建用户
        User user = new User();
        user.setPhone(phone);

        // 2.保存用户
        save(user);
        return user;
    }
}

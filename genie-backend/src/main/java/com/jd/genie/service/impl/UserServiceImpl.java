package com.jd.genie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jd.genie.entity.User;
import com.jd.genie.mapper.UserMapper;
import com.jd.genie.model.req.LoginReq;
import com.jd.genie.model.req.RegisterReq;
import com.jd.genie.model.response.LoginResp;
import com.jd.genie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public LoginResp login(LoginReq req) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, req.getUsername())
        );
        if(user == null){
            throw new RuntimeException("用户不存在");
        }
        String encryptedPwd = encryptPassword(req.getPassword());
        if(!encryptedPwd.equals(user.getPassword())){
            throw new RuntimeException("密码错误");
        }
        String token = generateToken(user.getId());
        // token 有效期 7 天
        redisTemplate.opsForValue().set("token:" + token, String.valueOf(user.getId()), 7, TimeUnit.DAYS);
        return LoginResp.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .token(token)
                .build();
    }

    @Override
    public LoginResp register(RegisterReq req) {
        User existUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, req.getUsername())
        );
        if(existUser != null){
            throw new RuntimeException("用户已存在");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(encryptPassword(req.getPassword()));
        user.setNickname(req.getNickname() != null ? req.getNickname() : req.getUsername());
        user.setEmail(req.getEmail());
        user.setStatus(1);
        user.setYn(1);

        userMapper.insert(user);

        String token = generateToken(user.getId());
        // token 有效期 7 天
        redisTemplate.opsForValue().set("token:" + token, String.valueOf(user.getId()), 7, TimeUnit.DAYS);
        return LoginResp.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .token(token)
                .build();
    }

    private String encryptPassword(String password) {
        return DigestUtils.md5DigestAsHex(
                ("genie_salt_" + password).getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public boolean validateToken(String token) {
        return token != null && redisTemplate.opsForValue().get("token:" + token) != null;
    }

    @Override
    public Long getUserIdFromToken(String token) {
        Object o = redisTemplate.opsForValue().get("token:" + token);
        if (o == null) {
            return null;
        }
        
        // 如果是字符串，尝试解析为 Long
        if (o instanceof String) {
            try {
                return Long.parseLong((String) o);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        // 其他情况返回 null
        return null;
    }
    private String generateToken(Long userId) {
        String raw = userId + "_" + System.currentTimeMillis() + "_" + Math.random();
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}

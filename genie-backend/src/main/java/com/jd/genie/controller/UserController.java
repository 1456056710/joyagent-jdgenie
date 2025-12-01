package com.jd.genie.controller;

import com.jd.genie.model.req.LoginReq;
import com.jd.genie.model.req.RegisterReq;
import com.jd.genie.model.response.LoginResp;
import com.jd.genie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginReq req){
        Map<String, Object> res = new HashMap<>();
        try{
            LoginResp resp = userService.login(req);
            res.put("code", 200);
            res.put("data", resp);
            res.put("message", "登陆成功");
        } catch (Exception e){
            res.put("code", 500);
            res.put("message", e.getMessage());
        }
        return res;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody RegisterReq req){
        Map<String, Object> res = new HashMap<>();
        try{
            LoginResp resp = userService.register(req);
            res.put("code", 200);
            res.put("data", resp);
            res.put("message", "注册成功");
        } catch (Exception e){
            res.put("code", 500);
            res.put("message", e.getMessage());
        }
        return res;
    }

    @GetMapping("/info")
    public Map<String, Object> getUserInfo(@RequestHeader(value = "Authorization", required = false) String token) {
        Map<String, Object> result = new HashMap<>();
        if (token == null || !userService.validateToken(token)) {
            result.put("code", 401);
            result.put("message", "未登录或token已过期");
            return result;
        }
        Long userId = userService.getUserIdFromToken(token);
        result.put("code", 200);
        result.put("data", Map.of("userId", userId));
        return result;
    }
}

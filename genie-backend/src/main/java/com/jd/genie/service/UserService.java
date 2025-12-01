package com.jd.genie.service;

import com.jd.genie.model.req.LoginReq;
import com.jd.genie.model.req.RegisterReq;
import com.jd.genie.model.response.LoginResp;

public interface UserService {

    LoginResp login(LoginReq req);
    LoginResp register(RegisterReq req);
    boolean validateToken(String token);
    Long getUserIdFromToken(String token);
}

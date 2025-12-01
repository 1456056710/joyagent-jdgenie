package com.jd.genie.model.req;

import lombok.Data;

@Data
public class RegisterReq {
    private String username;
    private String password;
    private String email;
    private String nickname;
}

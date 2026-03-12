package com.mes.admin.domain.vo;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class UserInfoVO {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private String factoryCode;
    private List<String> roles;
    private Set<String> permissions;
}

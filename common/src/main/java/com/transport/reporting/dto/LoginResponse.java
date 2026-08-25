package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LoginResponse {

    private String token;
    private String tokenType;
    private Long userId;
    private String username;
    private String name;
    private String email;
    private List<String> roles;
    private List<String> permissions;
    private List<MenuItemResponse> menus;
}

package com.matheusgn.ecommerce.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminLoginResponse {

    private boolean authenticated;
    private String role;
    private String username;
    private String accessToken;
    private String tokenType;
}

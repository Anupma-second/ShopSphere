package com.shopsphere.ecommerce.dto;

public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private UserResponse user;

    public AuthResponse() {
    }

    public AuthResponse(
            String accessToken,
            String refreshToken,
            UserResponse user) {

        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public UserResponse getUser() {
        return user;
    }
}

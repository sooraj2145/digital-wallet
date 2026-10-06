package com.wallet.dto;

public record LoginResponse(
        String token,
        String tokenType
) {
}

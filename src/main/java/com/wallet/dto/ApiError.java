package com.wallet.dto;

public record ApiError(
        int status,
        String error
) {
}

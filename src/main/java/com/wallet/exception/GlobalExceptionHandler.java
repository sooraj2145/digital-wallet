package com.wallet.exception;


import com.wallet.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WalletNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleWalletNotFound(WalletNotFoundException ex) {

        return new ApiError(
                404,
                ex.getMessage()
        );
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleInsufficientBalance(InsufficientBalanceException ex) {
        return new ApiError(
                400,
                ex.getMessage()
        );
    }

    @ExceptionHandler(InvalidTransferException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleInvalidTransfer(InvalidTransferException ex) {
        return new ApiError(
                400,
                ex.getMessage()
        );
    }
}

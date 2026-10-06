package com.wallet.exception;


import com.wallet.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    @ExceptionHandler(IdempotencyConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleIdempotencyConflict(IdempotencyConflictException ex) {
        return new ApiError(
                409,
                ex.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField() + ": " + error.getDefaultMessage()
                )
                .findFirst()
                .orElse("Validation failed");

        return new ApiError(
                400,
                errorMessage
        );
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleEmailAlreadyRegistered(
            EmailAlreadyRegisteredException exception
    ) {
        return new ApiError(
                409,
                exception.getMessage()
        );
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiError handleAuthenticationFailed(
            AuthenticationFailedException exception
    ) {
        return new ApiError(
                401,
                exception.getMessage()
        );
    }


    @ExceptionHandler(GeminiServiceException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiError handleGeminiServiceException(
            GeminiServiceException exception
    ) {
        return new ApiError(
                503,
                exception.getMessage()
        );
    }
}

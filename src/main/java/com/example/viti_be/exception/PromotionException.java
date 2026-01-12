package com.example.viti_be.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Base exception cho Promotion module
 */
public class PromotionException extends RuntimeException {
    public PromotionException(String message) {
        super(message);
    }

    public PromotionException(String message, Throwable cause) {
        super(message, cause);
    }
}


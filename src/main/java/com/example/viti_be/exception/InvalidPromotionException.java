package com.example.viti_be.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus; /**
 * Promotion không hợp lệ (expired, inactive, etc.)
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPromotionException extends PromotionException {
    public InvalidPromotionException(String message) {
        super(message);
    }
}

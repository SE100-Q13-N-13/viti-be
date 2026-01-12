package com.example.viti_be.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus; /**
 * Validation error
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PromotionValidationException extends PromotionException {
    public PromotionValidationException(String message) {
        super(message);
    }
}

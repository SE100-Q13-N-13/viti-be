package com.example.viti_be.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus; /**
 * Promotion conflict
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class PromotionConflictException extends PromotionException {
    public PromotionConflictException(String message) {
        super(message);
    }
}

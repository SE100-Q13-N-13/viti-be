package com.example.viti_be.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus; /**
 * Promotion không tìm thấy
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class PromotionNotFoundException extends PromotionException {
    public PromotionNotFoundException(String code) {
        super("Promotion not found: " + code);
    }

    public PromotionNotFoundException(String field, Object value) {
        super(String.format("Promotion not found with %s: %s", field, value));
    }
}

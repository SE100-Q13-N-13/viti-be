package com.example.viti_be.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus; /**
 * Promotion code đã tồn tại
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicatePromotionCodeException extends PromotionException {
    public DuplicatePromotionCodeException(String code) {
        super("Promotion code already exists: " + code);
    }
}

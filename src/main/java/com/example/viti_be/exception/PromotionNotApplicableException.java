package com.example.viti_be.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus; /**
 * Promotion không đủ điều kiện áp dụng
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PromotionNotApplicableException extends PromotionException {
    public PromotionNotApplicableException(String message) {
        super(message);
    }
}

package com.example.viti_be.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus; /**
 * Promotion đã hết quota
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PromotionQuotaExceededException extends PromotionException {
    public PromotionQuotaExceededException(String code) {
        super("Promotion quota exceeded: " + code);
    }
}

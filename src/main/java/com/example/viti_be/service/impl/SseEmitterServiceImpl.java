package com.example.viti_be.service.impl;

import com.example.viti_be.service.SseEmitterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementation của SseEmitterService.
 * Quản lý SSE connections cho realtime notifications.
 * 
 * Thread-safe sử dụng CopyOnWriteArrayList để xử lý concurrent access.
 */
@Service
@Slf4j
public class SseEmitterServiceImpl implements SseEmitterService {

    // Default timeout: 30 phút (có thể config nếu cần)
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    // Thread-safe list để lưu các emitters
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @Override
    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // Đăng ký các callbacks để cleanup khi connection kết thúc
        emitter.onCompletion(() -> {
            log.debug("SSE connection completed");
            removeEmitter(emitter);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE connection timed out");
            emitter.complete();
            removeEmitter(emitter);
        });

        emitter.onError(e -> {
            log.debug("SSE connection error: {}", e.getMessage());
            removeEmitter(emitter);
        });

        emitters.add(emitter);
        log.info("New SSE emitter registered. Total active: {}", emitters.size());

        // Gửi event khởi tạo để xác nhận connection thành công
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE connection established"));
        } catch (IOException e) {
            log.error("Failed to send initial SSE event", e);
            removeEmitter(emitter);
        }

        return emitter;
    }

    @Override
    public void sendEventToAll(String eventName, Object data) {
        log.debug("Sending SSE event '{}' to {} subscribers", eventName, emitters.size());

        // Iterate và gửi event, remove emitter nếu gặp lỗi
        List<SseEmitter> deadEmitters = new java.util.ArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                log.debug("Failed to send SSE event to emitter, marking for removal");
                deadEmitters.add(emitter);
            }
        }

        // Cleanup dead emitters
        deadEmitters.forEach(this::removeEmitter);

        log.debug("SSE event '{}' sent successfully to {} subscribers",
                eventName, emitters.size() - deadEmitters.size());
    }

    @Override
    public void removeEmitter(SseEmitter emitter) {
        if (emitters.remove(emitter)) {
            log.debug("SSE emitter removed. Total active: {}", emitters.size());
        }
    }

    @Override
    public int getActiveEmitterCount() {
        return emitters.size();
    }
}

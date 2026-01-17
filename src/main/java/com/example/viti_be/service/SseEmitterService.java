package com.example.viti_be.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Service interface for SSE (Server-Sent Events) management.
 * Quản lý kết nối SSE để push notifications realtime cho admin.
 */
public interface SseEmitterService {

    /**
     * Tạo và đăng ký một SseEmitter mới cho admin.
     * Emitter này sẽ được sử dụng để push notification events.
     *
     * @return SseEmitter mới được tạo
     */
    SseEmitter createEmitter();

    /**
     * Gửi một event tới tất cả admin đang subscribe.
     *
     * @param eventName Tên của event (ví dụ: "ORDER_NEW")
     * @param data      Dữ liệu gửi kèm event
     */
    void sendEventToAll(String eventName, Object data);

    /**
     * Xóa một emitter khỏi danh sách (khi disconnect/timeout/error).
     *
     * @param emitter Emitter cần xóa
     */
    void removeEmitter(SseEmitter emitter);

    /**
     * Lấy số lượng admin đang subscribe.
     *
     * @return Số lượng emitter đang active
     */
    int getActiveEmitterCount();
}

package com.example.viti_be.controller;

import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.dto.response.NotificationResponse;
import com.example.viti_be.dto.response.UnreadCountResponse;
import com.example.viti_be.dto.response.pagnitation.PageResponse;
import com.example.viti_be.service.NotificationService;
import com.example.viti_be.service.SseEmitterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * Controller xử lý các API liên quan đến Notification.
 * Bao gồm SSE endpoint và REST APIs cho admin.
 */
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(
    name = "Notification Management", 
    description = """
        ## Hệ thống Thông báo Realtime
        
        Hệ thống notification sử dụng **Server-Sent Events (SSE)** để push thông báo realtime kết hợp với REST APIs để quản lý.
        
        ### Luồng hoạt động
        1. **Frontend subscribe SSE stream** khi admin vào trang quản trị (login)
        2. **Server push event** khi có sự kiện mới (đơn hàng mới, cảnh báo tồn kho, etc.)
        3. **Frontend nhận event** và hiển thị notification popup/toast
        4. **Frontend gọi REST API** để lấy danh sách, đánh dấu đã đọc
        
        ### Event types hiện tại
        - `ORDER_NEW`: Có đơn hàng mới được tạo
        
        ### Lưu ý kỹ thuật
        - SSE connection timeout: 30 phút
        - Cần reconnect khi connection bị đóng
        - Event chỉ push cho admin đang online
        """
)
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterService sseEmitterService;

    // ===============================
    // SSE ENDPOINT
    // ===============================

    /**
     * SSE endpoint để admin subscribe nhận thông báo realtime.
     * Frontend cần tạo EventSource connection tới endpoint này.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Subscribe SSE stream để nhận notification realtime",
            description = """
                **Mục đích:** Tạo kết nối SSE để nhận thông báo realtime khi có sự kiện mới.
                
                **Khi nào sử dụng:**
                - Khi admin vào trang quản trị (login)
                - Reconnect khi connection bị đóng
                
                **Response:** Stream of Server-Sent Events
                - Event `connected`: Connection established
                - Event `ORDER_NEW`: New order notification
                - Data format: JSON của NotificationResponse
                """
    )
    public SseEmitter streamNotifications() {
        return sseEmitterService.createEmitter();
    }

    // ===============================
    // REST APIs
    // ===============================

    /**
     * Lấy danh sách notifications với phân trang.
     * Sử dụng cấu trúc PageResponse của hệ thống.
     */
    @GetMapping
    @Operation(
            summary = "Lấy danh sách notifications (phân trang)",
            description = """
                **Mục đích:** Lấy danh sách tất cả notifications với phân trang, sắp xếp theo thời gian mới nhất.
                
                **Khi nào sử dụng:**
                - Load trang notification lần đầu
                - User click "Xem tất cả thông báo"
                - Pagination khi scroll/click next page
                - Sau khi nhận SSE event (optional - refresh list)
                
                
                
                **Query Parameters:**
                - `page`: Số trang (0-based, mặc định: 0)
                - `size`: Số lượng/trang (mặc định: 20)
                - `sort`: Sắp xếp (mặc định: createdAt,desc)
                
                **Response:** PageResponse chứa danh sách NotificationResponse
                """
    )
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getAllNotifications(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        PageResponse<NotificationResponse> notifications = notificationService.getAllNotifications(pageable);
        return ResponseEntity.ok(ApiResponse.success(notifications, "Lấy danh sách thông báo thành công"));
    }

    /**
     * Lấy chi tiết một notification.
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Lấy chi tiết một notification",
            description = """
                **Mục đích:** Lấy thông tin chi tiết của một notification cụ thể.
                
                **Khi nào sử dụng:**
                - Hiếm khi dùng (thông tin đã có trong danh sách)
                - Có thể dùng để refresh thông tin notification đơn lẻ
                
                
                
                **Response:** NotificationResponse object
                """
    )
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotificationById(
            @PathVariable UUID id
    ) {
        NotificationResponse notification = notificationService.getNotificationById(id);
        return ResponseEntity.ok(ApiResponse.success(notification, "Lấy thông báo thành công"));
    }

    /**
     * Lấy số lượng notification chưa đọc.
     */
    @GetMapping("/unread-count")
    @Operation(
            summary = "Lấy số lượng notification chưa đọc",
            description = """
                **Mục đích:** Lấy số lượng notifications chưa đọc để hiển thị badge.
                
                **Khi nào sử dụng:**
                - Load trang admin lần đầu
                - Sau khi nhận SSE event ORDER_NEW (tăng count)
                - Sau khi đánh dấu đã đọc (giảm count)
                - Polling định kỳ nếu không dùng SSE (không khuyến khích)
                
                
                
                **Response:** `{ "unreadCount": number }`
                """
    )
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount() {
        long count = notificationService.getUnreadCount();
        UnreadCountResponse response = UnreadCountResponse.builder()
                .unreadCount(count)
                .build();
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy số thông báo chưa đọc thành công"));
    }

    /**
     * Đánh dấu một notification đã đọc.
     */
    @PatchMapping("/{id}/read")
    @Operation(
            summary = "Đánh dấu một notification đã đọc",
            description = """
                **Mục đích:** Đánh dấu một notification cụ thể là đã đọc.
                
                **Khi nào sử dụng:**
                - User click vào notification trong dropdown
                - User hover notification sau vài giây (optional - auto mark)
                - User click để navigate tới chi tiết entity (order, product, etc.)
                
                
                
                **Response:** Success message (data = null)
                """
    )
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đánh dấu đã đọc thành công"));
    }

    /**
     * Đánh dấu tất cả notifications đã đọc.
     */
    @PatchMapping("/mark-all-read")
    @Operation(
            summary = "Đánh dấu tất cả notifications đã đọc",
            description = """
                **Mục đích:** Đánh dấu tất cả notifications là đã đọc.
                
                **Khi nào sử dụng:**
                - User click button "Đánh dấu tất cả đã đọc"
                - User muốn clear tất cả badge notifications
                
               
                
                **Response:** Success message (data = null)
                """
    )
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok(ApiResponse.success(null, "Đánh dấu tất cả đã đọc thành công"));
    }
}

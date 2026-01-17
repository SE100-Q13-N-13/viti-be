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
        1. **Frontend subscribe SSE stream** khi admin vào trang quản trị
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
                - Khi admin vào trang quản trị (mount component)
                - Reconnect khi connection bị đóng
                
                **Implementation Frontend:**
                ```javascript
                const eventSource = new EventSource('/api/admin/notifications/stream', {
                  headers: { 'Authorization': 'Bearer TOKEN' }
                });
                
                // Listen for connection established
                eventSource.addEventListener('connected', (e) => {
                  console.log('Connected:', e.data);
                });
                
                // Listen for new order notifications
                eventSource.addEventListener('ORDER_NEW', (e) => {
                  const notification = JSON.parse(e.data);
                  // Hiển thị toast/popup
                  showNotificationToast(notification);
                  // Tăng badge count
                  updateUnreadCount();
                });
                
                // Handle errors & reconnect
                eventSource.onerror = (error) => {
                  console.error('SSE Error:', error);
                  eventSource.close();
                  // Reconnect after delay
                  setTimeout(() => reconnectSSE(), 3000);
                };
                
                // Cleanup khi unmount
                return () => eventSource.close();
                ```
                
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
                
                **Implementation Frontend:**
                ```javascript
                // Load danh sách notification
                const fetchNotifications = async (page = 0, size = 20) => {
                  const response = await fetch(
                    `/api/admin/notifications?page=${page}&size=${size}&sort=createdAt,desc`,
                    { headers: { 'Authorization': 'Bearer TOKEN' } }
                  );
                  const data = await response.json();
                  
                  // Render danh sách
                  renderNotificationList(data.data.content);
                  
                  // Update pagination info
                  updatePagination({
                    currentPage: data.data.currentPage,
                    totalPages: data.data.totalPages,
                    totalElements: data.data.totalElements
                  });
                };
                ```
                
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
                
                **Implementation Frontend:**
                ```javascript
                const getNotificationDetail = async (notificationId) => {
                  const response = await fetch(
                    `/api/admin/notifications/${notificationId}`,
                    { headers: { 'Authorization': 'Bearer TOKEN' } }
                  );
                  const data = await response.json();
                  return data.data; // NotificationResponse
                };
                ```
                
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
                
                **Implementation Frontend:**
                ```javascript
                // Load unread count khi mount
                const fetchUnreadCount = async () => {
                  const response = await fetch(
                    '/api/admin/notifications/unread-count',
                    { headers: { 'Authorization': 'Bearer TOKEN' } }
                  );
                  const data = await response.json();
                  
                  // Update badge
                  updateBadge(data.data.unreadCount);
                };
                
                // Tăng count khi nhận SSE event
                eventSource.addEventListener('ORDER_NEW', () => {
                  setUnreadCount(prev => prev + 1);
                });
                
                // Giảm count khi mark as read
                const markAsReadAndUpdateCount = async (id) => {
                  await markAsRead(id);
                  setUnreadCount(prev => Math.max(0, prev - 1));
                };
                ```
                
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
                
                **Implementation Frontend:**
                ```javascript
                const markNotificationAsRead = async (notificationId) => {
                  await fetch(
                    `/api/admin/notifications/${notificationId}/read`,
                    {
                      method: 'PATCH',
                      headers: { 'Authorization': 'Bearer TOKEN' }
                    }
                  );
                  
                  // Update UI
                  updateNotificationStyle(notificationId, { isRead: true });
                  
                  // Giảm unread count
                  setUnreadCount(prev => Math.max(0, prev - 1));
                };
                
                // Tự động mark as read khi click
                const handleNotificationClick = async (notification) => {
                  // Mark as read
                  if (!notification.isRead) {
                    await markNotificationAsRead(notification.id);
                  }
                  
                  // Navigate tới entity detail
                  if (notification.entityType === 'ORDER') {
                    navigate(`/orders/${notification.entityId}`);
                  }
                };
                ```
                
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
                
                **Implementation Frontend:**
                ```javascript
                const markAllNotificationsAsRead = async () => {
                  await fetch(
                    '/api/admin/notifications/mark-all-read',
                    {
                      method: 'PATCH',
                      headers: { 'Authorization': 'Bearer TOKEN' }
                    }
                  );
                  
                  // Reset unread count về 0
                  setUnreadCount(0);
                  
                  // Update UI - mark tất cả notification là read
                  setNotifications(prev => 
                    prev.map(n => ({ ...n, isRead: true }))
                  );
                  
                  // Hide badge
                  hideBadge();
                };
                
                // UI Button
                <button onClick={markAllNotificationsAsRead}>
                  Đánh dấu tất cả đã đọc
                </button>
                ```
                
                **Response:** Success message (data = null)
                """
    )
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok(ApiResponse.success(null, "Đánh dấu tất cả đã đọc thành công"));
    }
}

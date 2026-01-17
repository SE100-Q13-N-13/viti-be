package com.example.viti_be.repository;

import com.example.viti_be.model.Notification;
import com.example.viti_be.model.model_enum.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Lấy danh sách notification cho admin (không filter theo user cụ thể)
     * Sắp xếp theo thời gian tạo mới nhất
     */
    @Query("SELECT n FROM Notification n WHERE n.isDeleted = false ORDER BY n.createdAt DESC")
    Page<Notification> findAllActiveNotifications(Pageable pageable);

    /**
     * Lấy danh sách notification theo user
     */
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.isDeleted = false ORDER BY n.createdAt DESC")
    Page<Notification> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Đếm số notification chưa đọc (cho admin - không filter user)
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.isRead = false AND n.isDeleted = false")
    long countUnreadNotifications();

    /**
     * Đếm số notification chưa đọc theo user
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false AND n.isDeleted = false")
    long countUnreadByUserId(@Param("userId") UUID userId);

    /**
     * Đánh dấu một notification đã đọc
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id")
    void markAsRead(@Param("id") UUID id);

    /**
     * Đánh dấu tất cả notification đã đọc (cho admin)
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.isDeleted = false AND n.isRead = false")
    void markAllAsRead();

    /**
     * Đánh dấu tất cả notification của user đã đọc
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") UUID userId);

    /**
     * Lấy notifications theo type
     */
    @Query("SELECT n FROM Notification n WHERE n.type = :type AND n.isDeleted = false ORDER BY n.createdAt DESC")
    Page<Notification> findByType(@Param("type") NotificationType type, Pageable pageable);
}

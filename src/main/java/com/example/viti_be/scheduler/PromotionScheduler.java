package com.example.viti_be.scheduler;

import com.example.viti_be.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks cho Promotion module
 *
 * Tasks:
 * - Auto-activate promotions khi start_date đến
 * - Auto-expire promotions khi end_date qua
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PromotionScheduler {

    private final PromotionService promotionService;

    /**
     * Auto-activate promotions every 5 minutes
     * Chạy mỗi 5 phút để kích hoạt các promotions đã đến thời gian bắt đầu
     */
    @Scheduled(cron = "0 */5 * * * *") // Mỗi 5 phút
    public void autoActivatePromotions() {
        try {
            log.debug("Running scheduled task: Auto-activate promotions");
            promotionService.autoActivatePromotions();
        } catch (Exception e) {
            log.error("Error in auto-activate promotions task", e);
        }
    }

    /**
     * Auto-expire promotions every 5 minutes
     * Chạy mỗi 5 phút để vô hiệu hóa các promotions đã hết hạn
     */
    @Scheduled(cron = "0 */5 * * * *") // Mỗi 5 phút
    public void autoExpirePromotions() {
        try {
            log.debug("Running scheduled task: Auto-expire promotions");
            promotionService.autoExpirePromotions();
        } catch (Exception e) {
            log.error("Error in auto-expire promotions task", e);
        }
    }

    /**
     * Daily promotion health check at 2 AM
     * Kiểm tra và báo cáo tình trạng promotions mỗi ngày
     */
    @Scheduled(cron = "0 0 2 * * *") // 2 AM mỗi ngày
    public void dailyPromotionHealthCheck() {
        try {
            log.info("Running scheduled task: Daily promotion health check");
            // TODO: Implement health check logic if needed
            // - Check for promotions about to expire
            // - Check for promotions with low quota
            // - Send notifications to admins
        } catch (Exception e) {
            log.error("Error in daily promotion health check task", e);
        }
    }
}
package com.example.viti_be.scheduler;

import com.example.viti_be.dto.response.ResetResultResponse;
import com.example.viti_be.repository.SystemConfigRepository;
import com.example.viti_be.service.LoyaltyPointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Scheduled job để tự động reset điểm tích lũy hàng năm
 * Chạy vào 00:00 mỗi ngày để kiểm tra xem có phải ngày reset không
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoyaltyPointResetScheduler {

    private final LoyaltyPointService loyaltyPointService;
    private final SystemConfigRepository systemConfigRepository;

    /**
     * Chạy lúc 00:00 hàng ngày
     * Cron: giây phút giờ ngày tháng thứ
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void checkAndResetPoints() {
        try {
            // Kiểm tra xem có bật auto reset không
            Boolean resetEnabled = getConfigBoolean("loyalty.reset.enabled", true);
            if (!resetEnabled) {
                log.debug("Loyalty reset is disabled, skipping");
                return;
            }

            // Kiểm tra xem hôm nay có phải ngày reset không
            if (shouldResetToday()) {
                log.info("Starting scheduled loyalty points reset...");

                String reason = String.format("Automatic annual reset - %s", LocalDate.now().getYear());
                ResetResultResponse result = loyaltyPointService.resetAllCustomerPoints(reason, null);

                log.info("Scheduled reset completed: {} customers, {} total points",
                        result.getTotalCustomersReset(), result.getTotalPointsReset());

                // Cập nhật next reset date
                updateNextResetDate();
            }
        } catch (Exception e) {
            log.error("Error during scheduled loyalty reset", e);
        }
    }

    /**
     * Kiểm tra xem hôm nay có phải ngày reset không
     * Mặc định: reset vào 01/01 hàng năm
     */
    private boolean shouldResetToday() {
        LocalDate today = LocalDate.now();

        // Lấy next reset date từ config
        String nextResetStr = systemConfigRepository.findByConfigKey("loyalty.reset.next_date")
                .map(config -> config.getConfigValue())
                .orElse(null);

        if (nextResetStr != null) {
            LocalDate nextResetDate = LocalDate.parse(nextResetStr);
            return today.isEqual(nextResetDate) || today.isAfter(nextResetDate);
        }

        // Fallback: Nếu chưa có config, reset vào 01/01
        return today.getMonthValue() == 1 && today.getDayOfMonth() == 1;
    }

    /**
     * Cập nhật next reset date (sau khi reset xong)
     */
    private void updateNextResetDate() {
        Integer resetPeriodMonths = getConfigInt("loyalty.reset.period_months", 12);
        LocalDateTime nextReset = LocalDateTime.now().plusMonths(resetPeriodMonths);

        systemConfigRepository.findByConfigKey("loyalty.reset.next_date")
                .ifPresentOrElse(
                        config -> {
                            config.setConfigValue(nextReset.toLocalDate().toString());
                            systemConfigRepository.save(config);
                        },
                        () -> {
                            // Tạo config mới nếu chưa có
                            var newConfig = new com.example.viti_be.model.SystemConfig();
                            newConfig.setConfigKey("loyalty.reset.next_date");
                            newConfig.setConfigValue(nextReset.toLocalDate().toString());
                            newConfig.setDataType("DATE");
                            newConfig.setDescription("Next scheduled loyalty points reset date");
                            systemConfigRepository.save(newConfig);
                        }
                );

        log.info("Next reset date updated to: {}", nextReset.toLocalDate());
    }

    // Helper methods
    private Boolean getConfigBoolean(String key, Boolean defaultValue) {
        return systemConfigRepository.findByConfigKey(key)
                .map(config -> Boolean.parseBoolean(config.getConfigValue()))
                .orElse(defaultValue);
    }

    private Integer getConfigInt(String key, Integer defaultValue) {
        return systemConfigRepository.findByConfigKey(key)
                .map(config -> Integer.parseInt(config.getConfigValue()))
                .orElse(defaultValue);
    }
}
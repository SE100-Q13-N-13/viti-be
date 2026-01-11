package com.example.viti_be.service;

import com.example.viti_be.model.User;
import com.example.viti_be.model.model_enum.UserStatus;
import com.example.viti_be.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(
        value = "viti.app.scheduledTasksEnabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ScheduledTaskService {

    @Autowired
    private UserRepository userRepository;

    @Value("${viti.app.accountCleanupHours:24}")
    private int cleanupHours;

    /**
     * Cleanup PENDING accounts older than 24 hours
     * Chạy mỗi 6 giờ một lần (có thể điều chỉnh)
     * Cron: 0 0 * /6 * * * = Chạy vào phút 0, giờ 0 của mỗi 6 giờ
     */
    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void cleanupUnverifiedAccounts() {
        log.info("Starting cleanup of unverified accounts...");

        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(cleanupHours);

        List<User> unverifiedUsers = userRepository.findByStatusAndCreatedAtBefore(
                UserStatus.PENDING,
                cutoffTime
        );

        if (!unverifiedUsers.isEmpty()) {
            log.info("Found {} unverified accounts to delete", unverifiedUsers.size());

            for (User user : unverifiedUsers) {
                log.info("Deleting unverified account: {} ({})", user.getEmail(), user.getCreatedAt());
                userRepository.delete(user);
            }

            log.info("Cleanup completed. Deleted {} accounts", unverifiedUsers.size());
        } else {
            log.info("No unverified accounts found for cleanup");
        }
    }
}
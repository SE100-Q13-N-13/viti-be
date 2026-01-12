package com.example.viti_be.util;

import com.example.viti_be.security.services.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Utility class để lấy thông tin user hiện tại từ SecurityContext
 */
@Component
public class SecurityUtils {

    /**
     * Lấy UUID của user đang đăng nhập
     * @return UUID của user, hoặc null nếu chưa đăng nhập
     */
    public static UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetailsImpl userDetails) {
            return userDetails.getId();
        }

        return null;
    }

    /**
     * Lấy UserDetailsImpl của user đang đăng nhập
     * @return UserDetailsImpl hoặc null
     */
    public static UserDetailsImpl getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetailsImpl userDetails) {
            return userDetails;
        }

        return null;
    }

    /**
     * Kiểm tra user có role cụ thể không
     * @param role Tên role (VD: "ROLE_ADMIN")
     * @return true nếu có role
     */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(role));
    }

    /**
     * Kiểm tra có phải admin không
     * @return true nếu là admin
     */
    public static boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }
}
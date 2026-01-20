package com.example.viti_be.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

/**
 * Request để thay thế toàn bộ roles của user
 * Xóa tất cả roles hiện tại và set roles mới
 * Hỗ trợ cả roleNames và roleIds
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplaceUserRolesRequest {

    private Set<String> roleNames; // ["ROLE_ADMIN"]

    private Set<UUID> roleIds; // [uuid1]

    /**
     * Validation: Phải có ít nhất 1 role mới
     */
    @NotEmpty(message = "At least one new role must be provided")
    public Set<?> getProvidedRoles() {
        if (roleNames != null && !roleNames.isEmpty()) {
            return roleNames;
        }
        if (roleIds != null && !roleIds.isEmpty()) {
            return roleIds;
        }
        throw new IllegalArgumentException("Either roleNames or roleIds must be provided");
    }
}
package com.example.viti_be.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

/**
 * Request để xóa nhiều roles khỏi user cùng lúc
 * Hỗ trợ cả roleNames và roleIds
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchRemoveRolesRequest {

    private Set<String> roleNames; // ["ROLE_EMPLOYEE", "ROLE_CASHIER"]

    private Set<UUID> roleIds; // [uuid1, uuid2]

    /**
     * Validation: Phải có ít nhất 1 trong 2
     */
    @NotEmpty(message = "At least one role must be provided")
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
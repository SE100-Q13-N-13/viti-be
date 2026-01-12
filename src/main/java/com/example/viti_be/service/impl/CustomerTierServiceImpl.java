package com.example.viti_be.service.impl;

import com.example.viti_be.dto.request.CustomerTierRequest;
import com.example.viti_be.dto.response.CustomerTierResponse;
import com.example.viti_be.model.Customer;
import com.example.viti_be.model.CustomerTier;
import com.example.viti_be.model.LoyaltyPoint;
import com.example.viti_be.model.model_enum.AuditAction;
import com.example.viti_be.model.model_enum.AuditModule;
import com.example.viti_be.repository.CustomerRepository;
import com.example.viti_be.repository.CustomerTierRepository;
import com.example.viti_be.repository.LoyaltyPointRepository;
import com.example.viti_be.service.AuditLogService;
import com.example.viti_be.service.CustomerTierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerTierServiceImpl implements CustomerTierService {

    private final CustomerTierRepository tierRepository;
    private final CustomerRepository customerRepository;
    private final LoyaltyPointRepository loyaltyPointRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public List<CustomerTierResponse> getAllTiers() {
        return tierRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerTierResponse> getActiveTiers() {
        return tierRepository.findAllActiveTiers().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerTierResponse getTierById(UUID id) {
        CustomerTier tier = tierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer tier not found"));
        return mapToResponse(tier);
    }

    @Override
    @Transactional
    public CustomerTierResponse createTier(CustomerTierRequest request, UUID adminId) {
        // Validate: Không cho trùng name
        if (tierRepository.findByNameAndIsDeletedFalse(request.getName()).isPresent()) {
            throw new RuntimeException("Tier with name '" + request.getName() + "' already exists");
        }

        // Validate: Không cho trùng minPoint
        List<CustomerTier> existingTiers = tierRepository.findAllActiveTiers();
        boolean duplicateMinPoint = existingTiers.stream()
                .anyMatch(t -> t.getMinPoint().equals(request.getMinPoint()));
        if (duplicateMinPoint) {
            throw new RuntimeException("Tier with min point " + request.getMinPoint() + " already exists");
        }

        CustomerTier tier = new CustomerTier();
        tier.setName(request.getName());
        tier.setMinPoint(request.getMinPoint());
        tier.setDiscountRate(request.getDiscountRate());
        tier.setDescription(request.getDescription());
        tier.setStatus("ACTIVE");

        CustomerTier savedTier = tierRepository.save(tier);

        // Audit log
        auditLogService.logSuccess(
                adminId,
                AuditModule.CONFIG,
                AuditAction.CREATE,
                savedTier.getId().toString(),
                "customerTier",
                null,
                String.format("Created tier: %s (minPoint=%d, discount=%s%%)",
                        savedTier.getName(), savedTier.getMinPoint(), savedTier.getDiscountRate())
        );

        log.info("Created new customer tier: {} by admin {}", savedTier.getName(), adminId);
        return mapToResponse(savedTier);
    }

    @Override
    @Transactional
    public CustomerTierResponse updateTier(UUID id, CustomerTierRequest request, UUID adminId) {
        CustomerTier tier = tierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer tier not found"));

        String oldValue = String.format("name=%s, minPoint=%d, discount=%s%%",
                tier.getName(), tier.getMinPoint(), tier.getDiscountRate());

        // Validate: Không cho trùng name (trừ chính nó)
        tierRepository.findByNameAndIsDeletedFalse(request.getName())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new RuntimeException("Tier with name '" + request.getName() + "' already exists");
                    }
                });

        // Validate: Không cho trùng minPoint (trừ chính nó)
        List<CustomerTier> existingTiers = tierRepository.findAllActiveTiers().stream()
                .filter(t -> !t.getId().equals(id))
                .collect(Collectors.toList());
        boolean duplicateMinPoint = existingTiers.stream()
                .anyMatch(t -> t.getMinPoint().equals(request.getMinPoint()));
        if (duplicateMinPoint) {
            throw new RuntimeException("Tier with min point " + request.getMinPoint() + " already exists");
        }

        tier.setName(request.getName());
        tier.setMinPoint(request.getMinPoint());
        tier.setDiscountRate(request.getDiscountRate());
        tier.setDescription(request.getDescription());

        CustomerTier updatedTier = tierRepository.save(tier);

        String newValue = String.format("name=%s, minPoint=%d, discount=%s%%",
                updatedTier.getName(), updatedTier.getMinPoint(), updatedTier.getDiscountRate());

        // Audit log
        auditLogService.logSuccess(
                adminId,
                AuditModule.CONFIG,
                AuditAction.UPDATE,
                updatedTier.getId().toString(),
                "customerTier",
                oldValue,
                newValue
        );

        // Nếu thay đổi minPoint → re-calculate customers
        recalculateAllCustomerTiers(adminId);

        log.info("Updated customer tier: {} by admin {}", updatedTier.getName(), adminId);
        return mapToResponse(updatedTier);
    }

    @Override
    @Transactional
    public void deleteTier(UUID id, UUID adminId) {
        CustomerTier tier = tierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer tier not found"));

        // Validate: Không cho xóa tier đang có customers
        long customerCount = customerRepository.countByTierAndIsDeletedFalse(tier);
        if (customerCount > 0) {
            throw new RuntimeException(
                    "Cannot delete tier '" + tier.getName() + "' because " + customerCount + " customers are using it. " +
                            "Please move customers to another tier first."
            );
        }

        tier.setIsDeleted(true);
        tier.setStatus("INACTIVE");
        tierRepository.save(tier);

        // Audit log
        auditLogService.logSuccess(
                adminId,
                AuditModule.CONFIG,
                AuditAction.DELETE,
                tier.getId().toString(),
                "customerTier",
                tier.getName(),
                "Deleted tier"
        );

        log.info("Deleted customer tier: {} by admin {}", tier.getName(), adminId);
    }

    @Override
    @Transactional
    public CustomerTierResponse toggleTierStatus(UUID id, UUID adminId) {
        CustomerTier tier = tierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer tier not found"));

        String oldStatus = tier.getStatus();
        String newStatus = "ACTIVE".equals(oldStatus) ? "INACTIVE" : "ACTIVE";
        tier.setStatus(newStatus);

        CustomerTier savedTier = tierRepository.save(tier);

        // Audit log
        auditLogService.logSuccess(
                adminId,
                AuditModule.CONFIG,
                AuditAction.UPDATE,
                tier.getId().toString(),
                "customerTier",
                "status=" + oldStatus,
                "status=" + newStatus
        );

        log.info("Toggled tier {} status: {} → {} by admin {}", tier.getName(), oldStatus, newStatus, adminId);
        return mapToResponse(savedTier);
    }

    @Override
    @Transactional
    public void recalculateAllCustomerTiers(UUID adminId) {
        List<Customer> customers = customerRepository.findByIsDeletedFalse();
        int updatedCount = 0;

        for (Customer customer : customers) {
            LoyaltyPoint loyaltyPoint = loyaltyPointRepository.findByCustomerId(customer.getId()).orElse(null);
            if (loyaltyPoint == null) continue;

            // Tìm tier phù hợp
            List<CustomerTier> tiers = tierRepository.findTiersByPoints(loyaltyPoint.getTotalPoints());
            if (tiers.isEmpty()) continue;

            CustomerTier newTier = tiers.get(0);

            // Nếu tier thay đổi, update
            if (customer.getTier() == null || !customer.getTier().getId().equals(newTier.getId())) {
                String oldTierName = customer.getTier() != null ? customer.getTier().getName() : "NONE";
                customer.setTier(newTier);
                customerRepository.save(customer);
                updatedCount++;
                log.debug("Customer {} tier updated: {} → {}", customer.getId(), oldTierName, newTier.getName());
            }
        }

        // Audit log
        auditLogService.logSuccess(
                adminId,
                AuditModule.CONFIG,
                AuditAction.UPDATE,
                "SYSTEM",
                "customerTiers",
                null,
                String.format("Recalculated all customer tiers. Updated: %d customers", updatedCount)
        );

        log.info("Recalculated all customer tiers. Updated {} customers by admin {}", updatedCount, adminId);
    }

    // ========== HELPER METHODS ==========

    private CustomerTierResponse mapToResponse(CustomerTier tier) {
        long customerCount = customerRepository.countByTierAndIsDeletedFalse(tier);

        return CustomerTierResponse.builder()
                .id(tier.getId())
                .name(tier.getName())
                .minPoint(tier.getMinPoint())
                .discountRate(tier.getDiscountRate())
                .description(tier.getDescription())
                .status(tier.getStatus())
                .customerCount((int) customerCount)
                .createdAt(tier.getCreatedAt())
                .updatedAt(tier.getUpdatedAt())
                .build();
    }
}
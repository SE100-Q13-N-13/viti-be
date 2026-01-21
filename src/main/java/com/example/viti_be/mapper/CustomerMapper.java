package com.example.viti_be.mapper;

import com.example.viti_be.dto.response.CustomerResponse;
import com.example.viti_be.model.Customer;
import com.example.viti_be.model.LoyaltyPoint;

import java.util.stream.Collectors;

public class CustomerMapper {
    public static CustomerResponse mapToCustomerResponse(Customer customer) {
        CustomerResponse response = CustomerResponse.builder()
                .id(customer.getId())
                .fullName(customer.getFullName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .totalPurchase(customer.getTotalPurchase())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();

        if (customer.getTier() != null) {
            response.setTier(CustomerResponse.CustomerTierResponse.builder()
                    .id(customer.getTier().getId())
                    .name(customer.getTier().getName())
                    .minPoint(customer.getTier().getMinPoint())
                    .discountRate(customer.getTier().getDiscountRate())
                    .description(customer.getTier().getDescription())
                    .status(customer.getTier().getStatus())
                    .build());
        }

        if (customer.getLoyaltyPoint() != null) {
            LoyaltyPoint lp = customer.getLoyaltyPoint();
            response.setLoyaltyPoint(CustomerResponse.LoyaltyPointResponse.builder()
                    .id(lp.getId())
                    .totalPoints(lp.getTotalPoints())
                    .pointsAvailable(lp.getPointsAvailable())
                    .pointsUsed(lp.getPointsUsed())
                    .pointRate(lp.getPointRate())
                    .lastEarnedAt(lp.getLastEarnedAt())
                    .lastUsedAt(lp.getLastUsedAt())
                    .build());
        }

        if (customer.getAddresses() != null) {
            response.setAddresses(customer.getAddresses().stream()
                    .filter(addr -> !Boolean.TRUE.equals(addr.getIsDeleted()))
                    .map(addr -> {
                        String detailAddress = addr.getStreet() + ", " +
                                addr.getCommune().getName() + ", " +
                                addr.getProvince().getName();
                        return CustomerResponse.AddressResponse.builder()
                                .id(addr.getId())
                                .street(addr.getStreet())
                                .commune(addr.getCommune().getName())
                                .communeCode(addr.getCommune().getCode())
                                .city(addr.getProvince().getName())
                                .provinceCode(addr.getProvince().getCode())
                                .detailAddress(detailAddress)
                                .type(addr.getType())
                                .isPrimary(addr.getIsPrimary())
                                .postalCode(addr.getPostalCode())
                                .build();
                    })
                    .collect(Collectors.toList()));
        }

        return response;
    }
}

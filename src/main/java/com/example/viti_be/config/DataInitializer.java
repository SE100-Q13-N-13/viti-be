package com.example.viti_be.config;

import com.example.viti_be.model.*;
import com.example.viti_be.model.composite_key.UserRoleId;
import com.example.viti_be.model.model_enum.UserStatus;
import com.example.viti_be.repository.*;
import com.example.viti_be.service.CustomerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerTierRepository customerTierRepository;

    @Autowired
    private LoyaltyPointRepository loyaltyPointRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Override
    public void run(String... args) {
        try {
            log.info("Starting data initialization...");

            createSystemConfigs();

            // Skip if admin user already exists (check by email)
            if (userRepository.findByEmail("admin@viti.com").isPresent()) {
                log.info("Data already initialized (admin user exists). Skipping...");
                return;
            }

            // 1. Create roles if not exist
            Role adminRole = createRoleIfNotExists("ROLE_ADMIN", "Administrator role");
            Role employeeRole = createRoleIfNotExists("ROLE_EMPLOYEE", "Employee role");
            Role customerRole = createRoleIfNotExists("ROLE_CUSTOMER", "Customer role");
            Role technicianRole = createRoleIfNotExists("ROLE_TECHNICIAN", "Technician role");
            Role cashierRole = createRoleIfNotExists("ROLE_CASHIER", "Cashier role");
            Role warehouseRole = createRoleIfNotExists("ROLE_WAREHOUSE", "Warehouse role");
            Role accountantRole = createRoleIfNotExists("ROLE_ACCOUNTANT", "Accountant role");


            // 2. Create customer tiers
            CustomerTier bronzeTier = createCustomerTier("Bronze", 0, BigDecimal.valueOf(0.05));
            CustomerTier silverTier = createCustomerTier("Silver", 1000, BigDecimal.valueOf(0.10));
            CustomerTier goldTier = createCustomerTier("Gold", 5000, BigDecimal.valueOf(0.15));
            CustomerTier platinumTier = createCustomerTier("Platinum", 20000, BigDecimal.valueOf(0.20));

            // 3. Create admin user
            User adminUser = createUser(
                    "Nguyễn Văn Admin",
                    "admin@viti.com",
                    "0901234567",
                    "Admin123!",
                    "MALE",
                    LocalDate.of(1985, 1, 1),
                    UserStatus.ACTIVE,
                    adminRole
            );

            // 4. Create employee user
            User employeeUser = createUser(
                    "Trần Thị Nhân Viên",
                    "employee@viti.com",
                    "0901234568",
                    "Employee123!",
                    "FEMALE",
                    LocalDate.of(1990, 5, 15),
                    UserStatus.ACTIVE,
                    employeeRole
            );

            User technicianUser = createUser(
                    "Kim Kỹ Thuật",
                    "technician@viti.com",
                    "0125786423",
                    "Tech123!",
                    "MALE",
                    LocalDate.of(1999, 5, 15),
                    UserStatus.ACTIVE,
                    technicianRole
            );

            // 5. Create sample customers with different tiers
            createCustomerUser(
                    "Lê Văn An",
                    "customer1@gmail.com",
                    "0912345678",
                    "Customer123!",
                    "MALE",
                    LocalDate.of(1995, 3, 20),
                    customerRole,
                    bronzeTier,
                    500000
            );

            createCustomerUser(
                    "Phạm Thị Bình",
                    "customer2@gmail.com",
                    "0912345679",
                    "Customer123!",
                    "FEMALE",
                    LocalDate.of(1992, 7, 10),
                    customerRole,
                    silverTier,
                    2500000
            );

            createCustomerUser(
                    "Hoàng Minh Châu",
                    "customer3@gmail.com",
                    "0912345680",
                    "Customer123!",
                    "MALE",
                    LocalDate.of(1988, 11, 25),
                    customerRole,
                    goldTier,
                    12000000
            );

            createCustomerUser(
                    "Võ Thị Dung",
                    "customer4@gmail.com",
                    "0912345681",
                    "Customer123!",
                    "FEMALE",
                    LocalDate.of(1993, 2, 14),
                    customerRole,
                    platinumTier,
                    35000000
            );

            log.info("Data initialization completed successfully!");
            log.info("Admin credentials - Email: admin@viti.com, Password: Admin123!");
            log.info("Employee credentials - Email: employee@viti.com, Password: Employee123!");
            log.info("Technician credentials - Email: technician@viti.com, Password: Technician123!");
            log.info("Sample customer credentials - Email: customer1@gmail.com, Password: Customer123!");

        } catch (Exception e) {
            log.error("Error during data initialization", e);
        }
    }

    private void createSystemConfigs() {
        createConfigIfNotExists(
                "PART_MARKUP_PERCENT",
                "30.00",
                "DECIMAL",
                "Markup % cho parts khi bán trong warranty (VD: 30.00 = 30%)"
        );

        createConfigIfNotExists(
                "WARRANTY_PERIOD_MONTHS",
                "12",
                "INTEGER",
                "Thời gian bảo hành mặc định (tháng)"
        );

        createConfigIfNotExists(
                "WARRANTY_RETURN_DAYS",
                "7",
                "INTEGER",
                "Số ngày được trả máy miễn phí sau khi sửa xong"
        );

        // Tax & Discount configs
        createConfigIfNotExists(
                "VAT_PERCENT",
                "10.00",
                "DECIMAL",
                "Thuế VAT (%)"
        );

        createConfigIfNotExists(
                "MAX_EMPLOYEE_DISCOUNT_PERCENT",
                "15.00",
                "DECIMAL",
                "Discount tối đa mà nhân viên có thể áp dụng (%)"
        );

        // Inventory configs
        createConfigIfNotExists(
                "LOW_STOCK_THRESHOLD",
                "10",
                "INTEGER",
                "Ngưỡng cảnh báo hết hàng"
        );

        createConfigIfNotExists(
                "AUTO_REORDER_ENABLED",
                "true",
                "BOOLEAN",
                "Tự động tạo purchase order khi hết hàng"
        );

        // Business configs
        createConfigIfNotExists(
                "LOYALTY_POINTS_PER_VND",
                "1000",
                "INTEGER",
                "Số VND để được 1 loyalty point (VD: 1000 = mỗi 1000đ được 1 điểm)"
        );

        createConfigIfNotExists(
                "MIN_ORDER_VALUE",
                "100000",
                "DECIMAL",
                "Giá trị đơn hàng tối thiểu (VND)"
        );

        log.info("Created system configs");
    }

    private void createConfigIfNotExists(String key, String value, String dataType, String description) {
        if (!systemConfigRepository.existsByConfigKey(key)) {
            SystemConfig config = SystemConfig.builder()
                    .configKey(key)
                    .configValue(value)
                    .dataType(dataType)
                    .description(description)
                    .isEncrypted(false)
                    .build();
            systemConfigRepository.save(config);
            log.info("Created system config: {} = {}", key, value);
        }
    }

    private Role createRoleIfNotExists(String roleName, String description) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(roleName);
                    role.setDescription(description);
                    Role savedRole = roleRepository.save(role);
                    log.info("Created role: {}", roleName);
                    return savedRole;
                });
    }

    private CustomerTier createCustomerTier(String name, Integer minPoint, BigDecimal discountRate) {
        CustomerTier tier = new CustomerTier();
        tier.setName(name);
        tier.setMinPoint(minPoint);
        tier.setDiscountRate(discountRate);
        tier.setStatus("ACTIVE");
        CustomerTier savedTier = customerTierRepository.save(tier);
        log.info("Created customer tier: {}", name);
        return savedTier;
    }

    private User createUser(String fullName, String email, String phone, String password,
                            String gender, LocalDate dob, UserStatus status, Role role) {
        User user = new User();
        user.setUsername(email);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setStatus(status);

        User savedUser = userRepository.save(user);

        // Create UserRole association
        UserRole userRole = new UserRole();
        UserRoleId userRoleId = new UserRoleId(savedUser.getId(), role.getId());
        userRole.setId(userRoleId);
        userRole.setUser(savedUser);
        userRole.setRole(role);

        savedUser.setUserRoles(Set.of(userRole));
        userRepository.save(savedUser);

        log.info("Created user: {} with role: {}", fullName, role.getName());
        return savedUser;
    }

    private void createCustomerUser(String fullName, String email, String phone, String password,
                                    String gender, LocalDate dob, Role customerRole,
                                    CustomerTier tier, long totalSpending) {
        // Create User first
        User user = createUser(fullName, email, phone, password, gender, dob, UserStatus.ACTIVE, customerRole);

        customerService.createCustomerForUser(user);

        log.info("Created customer: {} with tier: {} and {} spending", fullName, tier.getName(), totalSpending);
    }
}

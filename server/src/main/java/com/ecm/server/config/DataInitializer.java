package com.ecm.server.config;

import com.ecm.server.model.Account;
import com.ecm.server.model.Employee;
import com.ecm.server.model.Role;
import com.ecm.server.repository.AccountRepository;
import com.ecm.server.repository.EmployeeRepository;
import com.ecm.server.repository.PaymentMethodRepository;
import com.ecm.server.repository.RoleRepository;
import com.ecm.server.repository.ShippingMethodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final PaymentMethodRepository paymentMethodRepository;
    private final ShippingMethodRepository shippingMethodRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final List<DemoProduct> DEMO_PRODUCTS = List.of(
            new DemoProduct("ApexBlade 16 Gaming", "apexblade-16-gaming", "ApexGear", "apexgear", "laptop-gaming", 42_990_000L, 18, "APEX-BLADE16-4060", "AB16-4060", "Laptop gaming 16 inch cho game thủ và creator."),
            new DemoProduct("GearPC GVN Strike", "gearpc-gvn-strike", "GearPC", "gearpc", "pc-gvn", 32_990_000L, 12, "GEARPC-GVN-STRIKE", "GVN-STRIKE", "PC gaming lắp sẵn, cân bằng hiệu năng và nâng cấp."),
            new DemoProduct("CoreForge B760 Kit", "coreforge-b760-kit", "CoreForge", "coreforge", "main-cpu-vga", 12_990_000L, 25, "CORE-B760-I5", "B760-I5-KIT", "Combo mainboard và CPU cho bộ máy phổ thông."),
            new DemoProduct("AeroMesh Airflow Case", "aeromesh-airflow-case", "AeroMesh", "aeromesh", "case-nguon-tan", 2_490_000L, 30, "AERO-CASE-AIRFLOW", "AIRFLOW-MESH", "Case mesh tối ưu luồng gió cho dàn PC."),
            new DemoProduct("FlashVault NVMe 1TB", "flashvault-nvme-1tb", "FlashVault", "flashvault", "o-cung-ram-the-nho", 1_890_000L, 40, "FLASH-NVME-1TB", "FV-NVME-1T", "SSD NVMe tốc độ cao cho hệ điều hành và game."),
            new DemoProduct("PulseCast Creator Kit", "pulsecast-creator-kit", "PulseCast", "pulsecast", "loa-micro-webcam", 3_490_000L, 16, "PULSE-CREATOR-KIT", "CREATOR-KIT", "Bộ microphone và webcam cho học tập, streaming."),
            new DemoProduct("VisionPro QHD 180", "visionpro-qhd-180", "VisionPro", "visionpro", "man-hinh", 7_990_000L, 14, "VISION-QHD-180", "VP27-QHD180", "Màn hình QHD 180Hz cho gaming và làm việc."),
            new DemoProduct("KeyForge 75 Wireless", "keyforge-75-wireless", "KeyForge", "keyforge", "ban-phim", 1_890_000L, 32, "KEYFORGE-75-WL", "KF75-WIRELESS", "Bàn phím cơ layout 75 phần trăm, kết nối không dây."),
            new DemoProduct("SwiftAim Wireless", "swiftaim-wireless", "SwiftAim", "swiftaim", "chuot-lot-chuot", 990_000L, 45, "SWIFTAIM-WIRELESS", "SA-WL-01", "Chuột gaming không dây nhẹ và chính xác."),
            new DemoProduct("EchoCore Wireless", "echocore-wireless", "EchoCore", "echocore", "tai-nghe", 2_290_000L, 20, "ECHO-WIRELESS", "EC-WL-01", "Tai nghe gaming không dây với microphone rõ tiếng."),
            new DemoProduct("LevelUp Ergo Chair", "levelup-ergo-chair", "LevelUp", "levelup", "ghe-ban", 5_690_000L, 9, "LEVELUP-ERGO", "LU-ERGO-01", "Ghế công thái học cho góc máy làm việc dài giờ."),
            new DemoProduct("NetLink Wi-Fi 6", "netlink-wifi-6", "NetLink", "netlink", "phan-mem-mang", 1_490_000L, 24, "NETLINK-WIFI6", "NL-WIFI6-01", "Router Wi-Fi 6 ổn định cho nhà và văn phòng nhỏ."),
            new DemoProduct("GameDock Pro Controller", "gamedock-pro-controller", "GameDock", "gamedock", "phu-kien-console", 1_790_000L, 22, "GAMEDOCK-PRO", "GD-PRO-01", "Tay cầm không dây cho PC và console."),
            new DemoProduct("NovaBook Air 14", "novabook-air-14", "NovaTech", "novatech", "laptop", 24_990_000L, 13, "NOVABOOK-AIR14", "NBA14-01", "Laptop mỏng nhẹ cho học tập và công việc."),
            new DemoProduct("Apex Creator Station 2", "apex-creator-station-2", "ApexGear", "apexgear", "pc-de-ban", 45_990_000L, 8, "APEX-CREATOR-2", "ACST-2", "PC desktop cho dựng phim, thiết kế và render."),
            new DemoProduct("NovaPhone Edge", "novaphone-edge", "NovaTech", "novatech", "dien-thoai", 18_990_000L, 17, "NOVAPHONE-EDGE", "NPE-01", "Điện thoại hiệu năng cao với màn hình sắc nét."),
            new DemoProduct("VisionPro 4K Creator", "visionpro-4k-creator", "VisionPro", "visionpro", "man-hinh", 11_990_000L, 10, "VISION-4K-CREATOR", "VP32-4K", "Màn hình 4K màu chuẩn cho nội dung sáng tạo."),
            new DemoProduct("ByteForge DDR5 32GB", "byteforge-ddr5-32gb", "ByteForge", "byteforge", "o-cung-ram-the-nho", 2_890_000L, 38, "BYTE-DDR5-32", "BF-DDR5-32", "Kit RAM DDR5 32GB cho đa nhiệm và gaming."),
            new DemoProduct("AeroMesh PSU 750W", "aeromesh-psu-750w", "AeroMesh", "aeromesh", "case-nguon-tan", 2_290_000L, 28, "AERO-PSU-750", "PSU-750-GOLD", "Nguồn 750W hiệu suất cao cho PC gaming."),
            new DemoProduct("KeyForge TKL RGB", "keyforge-tkl-rgb", "KeyForge", "keyforge", "ban-phim", 1_290_000L, 35, "KEYFORGE-TKL-RGB", "KFTKL-RGB", "Bàn phím cơ TKL RGB gọn cho góc máy."),
            new DemoProduct("SwiftAim Pro Sensor", "swiftaim-pro-sensor", "SwiftAim", "swiftaim", "chuot-lot-chuot", 1_590_000L, 31, "SWIFTAIM-PRO", "SA-PRO-01", "Chuột gaming cảm biến chính xác cho thi đấu."),
            new DemoProduct("StreamLite Webcam Pro", "streamlite-webcam-pro", "StreamLite", "streamlite", "loa-micro-webcam", 1_990_000L, 19, "STREAM-WEBCAM-PRO", "SL-CAM-PRO", "Webcam Full HD cho họp trực tuyến và streaming."),
            new DemoProduct("EchoCore Studio", "echocore-studio", "EchoCore", "echocore", "tai-nghe", 3_290_000L, 15, "ECHO-STUDIO", "EC-STUDIO-01", "Tai nghe over-ear cho nghe nhạc và làm việc."),
            new DemoProduct("GameDock Wireless Pad", "gamedock-wireless-pad", "GameDock", "gamedock", "phu-kien-console", 1_490_000L, 26, "GAMEDOCK-WL-PAD", "GD-WL-01", "Tay cầm không dây nhỏ gọn cho nhiều nền tảng."),
            new DemoProduct("CoreForge Ryzen Kit", "coreforge-ryzen-kit", "CoreForge", "coreforge", "main-cpu-vga", 16_990_000L, 18, "CORE-RYZEN-KIT", "RYZEN-KIT-01", "Combo nền tảng Ryzen cho PC gaming hiệu năng cao."),
            new DemoProduct("GearPC GVN Creator", "gearpc-gvn-creator", "GearPC", "gearpc", "pc-gvn", 42_990_000L, 7, "GEARPC-GVN-CREATOR", "GVN-CREATOR", "PC GVN cho dựng hình và các ứng dụng nặng."),
            new DemoProduct("FlashVault DDR5 Kit", "flashvault-ddr5-kit", "FlashVault", "flashvault", "o-cung-ram-the-nho", 3_590_000L, 21, "FLASH-DDR5-KIT", "FV-DDR5-32", "Kit bộ nhớ tốc độ cao cho máy đa nhiệm."),
            new DemoProduct("NetLink Mesh Wi-Fi", "netlink-mesh-wifi", "NetLink", "netlink", "phan-mem-mang", 2_490_000L, 18, "NETLINK-MESH", "NL-MESH-01", "Bộ mesh Wi-Fi phủ sóng ổn định cho căn hộ."));

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Initialize default system roles
        Role adminRole = getOrCreateRole("ROLE_ADMIN");
        getOrCreateRole("ROLE_EMPLOYEE");
        getOrCreateRole("ROLE_CUSTOMER");
        seedPaymentMethods();
        seedShippingMethods();

        // 2. Initialize default admin user if not exists
        if (!accountRepository.existsByEmailIgnoreCase("admin@ecm.com")) {
            Account adminAccount = Account.builder()
                    .email("admin@ecm.com")
                    .phone("0900000001")
                    .passwordHash(passwordEncoder.encode("Admin@123"))
                    .role(adminRole)
                    .status("ACTIVE")
                    .build();
            Account savedAccount = accountRepository.save(adminAccount);

            Employee adminEmployee = Employee.builder()
                    .firstName("System")
                    .lastName("Administrator")
                    .gender("MALE")
                    .joinedAt(java.time.LocalDate.now())
                    .build();
            adminEmployee.setAccount(savedAccount);
            employeeRepository.save(adminEmployee);

            log.info("Initialized default administrator account [admin@ecm.com / Admin@123]");
        }

        seedDemoCatalog();
    }

    private Role getOrCreateRole(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(Role.builder().name(name).status("ACTIVE").build()));
    }

    private void seedPaymentMethods() {
        createPaymentMethodIfMissing("COD", "Cash on delivery");
        createPaymentMethodIfMissing("STRIPE_CARD", "Stripe card");
        createPaymentMethodIfMissing("BANK_TRANSFER", "Bank transfer");
    }

    private void createPaymentMethodIfMissing(String code, String name) {
        paymentMethodRepository.findByCodeIgnoreCase(code).orElseGet(() ->
                paymentMethodRepository.save(com.ecm.server.model.PaymentMethod.builder()
                        .code(code).name(name).status("ACTIVE").build()));
    }

    private void seedShippingMethods() {
        createShippingMethodIfMissing("STANDARD", "Standard delivery", 0L);
        createShippingMethodIfMissing("EXPRESS", "Express delivery", 30_000L);
        createShippingMethodIfMissing("SAME_DAY", "Same-day delivery", 50_000L);
    }

    private void createShippingMethodIfMissing(String code, String name, long fee) {
        shippingMethodRepository.findByCodeIgnoreCase(code).orElseGet(() ->
                shippingMethodRepository.save(com.ecm.server.model.ShippingMethod.builder()
                        .code(code).name(name).fee(fee).status("ACTIVE").build()));
    }

    /**
     * Provide a repeatable local catalog so the storefront can be exercised
     * without manually creating dozens of products through the admin UI. The
     * records are regular catalog rows and remain editable through the APIs.
     */
    private void seedDemoCatalog() {
        UUID creatorId = employeeRepository.findAll().stream()
                .findFirst()
                .map(Employee::getAccountId)
                .orElse(null);
        if (creatorId == null) {
            log.warn("Skipping demo catalog seed because no employee account exists");
            return;
        }

        for (DemoProduct demo : DEMO_PRODUCTS) {
            UUID categoryId = findCategoryId(demo.categorySeoName());
            if (categoryId == null) {
                log.warn("Skipping demo product {} because category {} does not exist", demo.seoName(), demo.categorySeoName());
                continue;
            }
            UUID brandId = upsertBrand(demo.brandName(), demo.brandSeoName());
            UUID productId = upsertProduct(demo, brandId, categoryId, creatorId);
            upsertVariant(demo, productId, creatorId);
        }
    }

    private UUID findCategoryId(String seoName) {
        return jdbcTemplate.query(
                        "SELECT id FROM categories WHERE seo_name = ? AND status = 'ACTIVE'",
                        (resultSet, rowNum) -> (UUID) resultSet.getObject("id"),
                        seoName)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private UUID upsertBrand(String name, String seoName) {
        return jdbcTemplate.queryForObject("""
                        INSERT INTO brands (name, seo_name, status)
                        VALUES (?, ?, 'ACTIVE')
                        ON CONFLICT (seo_name) DO UPDATE
                            SET name = EXCLUDED.name, status = 'ACTIVE'
                        RETURNING id
                        """, UUID.class, name, seoName);
    }

    private UUID upsertProduct(DemoProduct demo, UUID brandId, UUID categoryId, UUID creatorId) {
        return jdbcTemplate.queryForObject("""
                        INSERT INTO products
                            (name, seo_name, brand_id, category_id, specifications,
                             description, status, created_by)
                        VALUES (?, ?, ?, ?, CAST(? AS jsonb), ?, 'ACTIVE', ?)
                        ON CONFLICT (seo_name) DO UPDATE
                            SET name = EXCLUDED.name,
                                brand_id = EXCLUDED.brand_id,
                                category_id = EXCLUDED.category_id,
                                specifications = EXCLUDED.specifications,
                                description = EXCLUDED.description,
                                status = 'ACTIVE'
                        RETURNING id
                        """, UUID.class,
                demo.name(), demo.seoName(), brandId, categoryId,
                "{\"demo\":true,\"segment\":\"gear\"}", demo.description(), creatorId);
    }

    private void upsertVariant(DemoProduct demo, UUID productId, UUID creatorId) {
        jdbcTemplate.update("""
                        INSERT INTO product_variants
                            (product_id, list_price, quantity, sku, model,
                             description, warranty_months, release_at, status, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, 24, CURRENT_DATE, 'ACTIVE', ?)
                        ON CONFLICT (sku) DO UPDATE
                            SET product_id = EXCLUDED.product_id,
                                list_price = EXCLUDED.list_price,
                                quantity = EXCLUDED.quantity,
                                model = EXCLUDED.model,
                                description = EXCLUDED.description,
                                warranty_months = EXCLUDED.warranty_months,
                                release_at = EXCLUDED.release_at,
                                status = 'ACTIVE'
                        """, productId, demo.price(), demo.quantity(), demo.sku(),
                demo.model(), demo.description(), creatorId);
    }

    private record DemoProduct(
            String name,
            String seoName,
            String brandName,
            String brandSeoName,
            String categorySeoName,
            long price,
            int quantity,
            String sku,
            String model,
            String description
    ) {
    }
}

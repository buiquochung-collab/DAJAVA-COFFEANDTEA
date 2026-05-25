package com.example.demo.config;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NewsRepository newsRepository;
    @Autowired private IngredientRepository ingredientRepository;
    @Autowired private RecipeRepository recipeRepository;
    @Autowired private OrderDetailRepository orderDetailRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private PointHistoryRepository pointHistoryRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserVoucherRepository userVoucherRepository;
    @Autowired private VoucherRepository voucherRepository;
    @Autowired private SystemSettingRepository systemSettingRepository;
    @Autowired private InventoryLogRepository inventoryLogRepository;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @org.springframework.transaction.annotation.Transactional
    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            // Nếu đã có dữ liệu (đã seed rồi) thì bỏ qua không seed lại nữa để tránh mất dữ liệu trên web thật
            return;
        }

        seedSettings();
        seedUsers();
        Map<String, Ingredient> ingMap = seedIngredients();
        Map<String, Category> catMap = seedCategories();
        seedProductsAndRecipes(catMap, ingMap);
        seedVouchers();
        seedNews();
        seedOrdersToTriggerBestseller();
    }

    private void seedSettings() {
        saveSetting("hotline", "1900 633 633", "Hotline CSKH", "GENERAL");
        saveSetting("address", "Phuc Long Tea & Coffee, Ho Chi Minh City", "Địa chỉ trụ sở", "GENERAL");
        saveSetting("point_rate", "10000", "Số tiền (đ) tương ứng 1 điểm", "LOYALTY");
        saveSetting("rank_gold", "100", "Điểm thăng hạng Gold", "LOYALTY");
        saveSetting("rank_platinum", "500", "Điểm thăng hạng Platinum", "LOYALTY");
        saveSetting("banner_main", "/uploads/news/tintuc1.png", "Banner chính", "APPEARANCE");
        saveSetting("banner_promo", "/uploads/news/tintuc2.png", "Banner khuyến mãi", "APPEARANCE");
    }

    private void saveSetting(String key, String value, String desc, String group) {
        systemSettingRepository.save(SystemSetting.builder().key(key).value(value).description(desc).groupName(group).build());
    }

    private void seedUsers() {
        // Admin
        userRepository.save(User.builder().username("admin").password(passwordEncoder.encode("admin123")).email("admin@fandb.com").role(User.Role.ADMIN).status("ACTIVE").membershipRank("PLATINUM").build());
        
        // Cashier
        userRepository.save(User.builder().username("cashier").password(passwordEncoder.encode("cashier123")).email("cashier@fandb.com").role(User.Role.CASHIER).status("ACTIVE").membershipRank("MEMBER").build());
        
        // Stock Manager
        userRepository.save(User.builder().username("stock").password(passwordEncoder.encode("stock123")).email("stock@fandb.com").role(User.Role.STOCK_MANAGER).status("ACTIVE").membershipRank("MEMBER").build());

        // Regular User
        userRepository.save(User.builder().username("user").password(passwordEncoder.encode("user123")).email("user@gmail.com").role(User.Role.USER).status("ACTIVE").membershipRank("SILVER").points(150).birthDate(java.time.LocalDate.now()).build());
    }

    private Map<String, Ingredient> seedIngredients() {
        List<Ingredient> ings = Arrays.asList(
            // TRÀ
            Ingredient.builder().name("Trà Lài (20 gói)").unit("bịch").stockQuantity(120.0).costPerUnit(BigDecimal.valueOf(50000)).build(),
            Ingredient.builder().name("Trà Sen (20 gói)").unit("bịch").stockQuantity(120.0).costPerUnit(BigDecimal.valueOf(50000)).build(),
            Ingredient.builder().name("Trà Ô long (20 gói)").unit("bịch").stockQuantity(120.0).costPerUnit(BigDecimal.valueOf(60000)).build(),
            Ingredient.builder().name("Trà Phúc Long (20 gói)").unit("bịch").stockQuantity(120.0).costPerUnit(BigDecimal.valueOf(70000)).build(),
            Ingredient.builder().name("Trà Quế hoa (20 gói)").unit("bịch").stockQuantity(120.0).costPerUnit(BigDecimal.valueOf(60000)).build(),
            Ingredient.builder().name("Trà Thảo mộc (20 gói)").unit("bịch").stockQuantity(120.0).costPerUnit(BigDecimal.valueOf(65000)).build(),
            Ingredient.builder().name("Hồng trà (200g)").unit("bịch").stockQuantity(100.0).costPerUnit(BigDecimal.valueOf(45000)).build(),
            Ingredient.builder().name("Trà Bá tước (200g)").unit("bịch").stockQuantity(100.0).costPerUnit(BigDecimal.valueOf(55000)).build(),

            // CÀ PHÊ & BỘT & SYRUP
            Ingredient.builder().name("Cà phê hạt (500g)").unit("bịch").stockQuantity(150.0).costPerUnit(BigDecimal.valueOf(110000)).build(),
            Ingredient.builder().name("Bột kem béo (kg)").unit("kg").stockQuantity(100.0).costPerUnit(BigDecimal.valueOf(85000)).build(),
            Ingredient.builder().name("Bột Matcha (kg)").unit("kg").stockQuantity(20.0).costPerUnit(BigDecimal.valueOf(650000)).build(),
            Ingredient.builder().name("Bột Chocolate (kg)").unit("kg").stockQuantity(50.0).costPerUnit(BigDecimal.valueOf(180000)).build(),
            Ingredient.builder().name("Syrup Vải (Chai)").unit("chai").stockQuantity(100.0).costPerUnit(BigDecimal.valueOf(125000)).build(),
            Ingredient.builder().name("Syrup Đào (Chai)").unit("chai").stockQuantity(100.0).costPerUnit(BigDecimal.valueOf(125000)).build(),
            Ingredient.builder().name("Syrup Thơm (Chai)").unit("chai").stockQuantity(100.0).costPerUnit(BigDecimal.valueOf(125000)).build(),
            Ingredient.builder().name("Syrup Ổi (Chai)").unit("chai").stockQuantity(100.0).costPerUnit(BigDecimal.valueOf(125000)).build(),
            Ingredient.builder().name("Sunup Ổi (Chai)").unit("chai").stockQuantity(50.0).costPerUnit(BigDecimal.valueOf(210000)).build(),
            Ingredient.builder().name("Syrup Chocolate (Chai)").unit("chai").stockQuantity(50.0).costPerUnit(BigDecimal.valueOf(195000)).build(),

            // TƯƠI & ĐỒ HỘP
            Ingredient.builder().name("Đác thơm (1kg)").unit("bịch").stockQuantity(50.0).costPerUnit(BigDecimal.valueOf(95000)).build(),
            Ingredient.builder().name("Trái thơm (Quả)").unit("trái").stockQuantity(30.0).costPerUnit(BigDecimal.valueOf(15000)).build(),
            Ingredient.builder().name("Chanh tươi (kg)").unit("kg").stockQuantity(20.0).costPerUnit(BigDecimal.valueOf(35000)).build(),
            Ingredient.builder().name("Cam tươi (kg)").unit("kg").stockQuantity(20.0).costPerUnit(BigDecimal.valueOf(45000)).build(),
            Ingredient.builder().name("Trái xoài (Quả)").unit("trái").stockQuantity(30.0).costPerUnit(BigDecimal.valueOf(20000)).build(),
            Ingredient.builder().name("Đào ngâm (Hộp)").unit("hộp").stockQuantity(100.0).costPerUnit(BigDecimal.valueOf(45000)).build(),
            Ingredient.builder().name("Vải ngâm (Hộp)").unit("hộp").stockQuantity(100.0).costPerUnit(BigDecimal.valueOf(48000)).build(),
            Ingredient.builder().name("Nhãn ngâm (Hộp)").unit("hộp").stockQuantity(100.0).costPerUnit(BigDecimal.valueOf(48000)).build(),

            // SỮA & KHÁC
            Ingredient.builder().name("Sữa đặc (Lon)").unit("hộp").stockQuantity(200.0).costPerUnit(BigDecimal.valueOf(18000)).build(),
            Ingredient.builder().name("Sữa tươi (Lít)").unit("lít").stockQuantity(100.0).costPerUnit(BigDecimal.valueOf(32000)).build(),
            Ingredient.builder().name("Đường cát (kg)").unit("kg").stockQuantity(50.0).costPerUnit(BigDecimal.valueOf(22000)).build(),
            Ingredient.builder().name("Bánh Oreo (12 cái)").unit("thanh").stockQuantity(50.0).costPerUnit(BigDecimal.valueOf(15000)).build(),
            Ingredient.builder().name("Sữa chua (200g)").unit("hộp").stockQuantity(50.0).costPerUnit(BigDecimal.valueOf(8000)).build()
        );        ingredientRepository.saveAll(ings);
        return ings.stream().collect(Collectors.toMap(Ingredient::getName, i -> i));
    }

    private Map<String, Category> seedCategories() {
        Category thucUong = categoryRepository.save(new Category(null, "Thức uống", null, "Giải khát", null));
        Category traSua = categoryRepository.save(new Category(null, "Trà sữa", null, null, thucUong));
        Category traTraiCay = categoryRepository.save(new Category(null, "Trà trái cây", null, null, thucUong));
        Category caPhe = categoryRepository.save(new Category(null, "Cà phê", null, null, thucUong));
        Category daXay = categoryRepository.save(new Category(null, "Đá xay", null, null, thucUong));
        Category banhNgotParent = categoryRepository.save(new Category(null, "Bánh ngọt", null, "Bánh nướng", null));
        Category banhMan = categoryRepository.save(new Category(null, "Bánh mặn", null, null, banhNgotParent));
        Category banhNgotChild = categoryRepository.save(new Category(null, "Bánh ngọt ", null, null, banhNgotParent));
        return Arrays.asList(thucUong, traSua, traTraiCay, caPhe, daXay, banhNgotParent, banhMan, banhNgotChild)
                .stream().collect(Collectors.toMap(Category::getName, c -> c));
    }

    private void seedProductsAndRecipes(Map<String, Category> catMap, Map<String, Ingredient> ingMap) {
        // --- 1. TRÀ SỮA ---
        List<String> tsList = Arrays.asList("Phúc Long", "Ô long", "Lài", "Sen", "Nhãn sen", "Bá tước", "Quế hoa", "Hồng trà");
        for (String name : tsList) {
            String fullName = name.contains("Hồng trà") ? "Hồng trà sữa" : "Trà sữa " + name;
            String desc = "Hương vị " + name + " đặc trưng hòa quyện cùng sữa béo ngậy.";
            Product p = saveProduct(fullName, 55000, 45000, catMap.get("Trà sữa"), desc);
            
            // Công thức Trà sữa
            String teaKey = name.equals("Bá tước") ? "Trà Bá tước (200g)" : 
                           (name.equals("Hồng trà") ? "Hồng trà (200g)" : 
                           (name.equals("Nhãn sen") ? "Trà Sen (20 gói)" : "Trà " + name + " (20 gói)"));
            addRecipe(p, ingMap.get(teaKey), teaKey.contains("200g") ? 0.1 : 0.05);
            addRecipe(p, ingMap.get("Sữa đặc (Lon)"), 0.05);
            addRecipe(p, ingMap.get("Bột kem béo (kg)"), 0.02);
            
            if (name.equals("Nhãn sen")) {
                addRecipe(p, ingMap.get("Nhãn ngâm (Hộp)"), 3.0/45.0);
            }
        }

        // --- 2. TRÀ TRÁI CÂY ---
        List<String> fruitTeas = Arrays.asList("Trà vải Lài", "Trà vải Sen", "Trà nhãn Lài", "Trà nhãn Sen");
        for (String name : fruitTeas) {
            String desc = "Sự kết hợp thanh mát giữa trà truyền thống và trái cây tươi mọng.";
            Product p = saveProduct(name, 60000, 50000, catMap.get("Trà trái cây"), desc);
            String teaType = name.contains("Lài") ? "Trà Lài (20 gói)" : "Trà Sen (20 gói)";
            addRecipe(p, ingMap.get(teaType), 0.05);
            addRecipe(p, ingMap.get("Syrup Vải (Chai)"), 0.03);
            addRecipe(p, ingMap.get("Đường cát (kg)"), 0.02);
            addRecipe(p, ingMap.get("Chanh tươi (kg)"), 1.0/120.0);
            
            if (name.contains("vải")) addRecipe(p, ingMap.get("Vải ngâm (Hộp)"), 3.0/20.0);
            if (name.contains("nhãn")) addRecipe(p, ingMap.get("Nhãn ngâm (Hộp)"), 3.0/45.0);
        }

        // Trà lài đác thơm
        Product dt = saveProduct("Trà lài đác thơm", 65000, 55000, catMap.get("Trà trái cây"), "Món trà giải nhiệt cực đỉnh với hạt đác và thơm tươi.");
        addRecipe(dt, ingMap.get("Trà Lài (20 gói)"), 0.05);
        addRecipe(dt, ingMap.get("Syrup Thơm (Chai)"), 0.03);
        addRecipe(dt, ingMap.get("Đường cát (kg)"), 0.02);
        addRecipe(dt, ingMap.get("Đác thơm (1kg)"), 0.05);
        addRecipe(dt, ingMap.get("Trái thơm (Quả)"), 0.05);

        // Thảo mộc
        Product tm = saveProduct("Trà thảo mộc", 65000, 55000, catMap.get("Trà trái cây"), "Hương vị thảo mộc tự nhiên, tốt cho sức khỏe.");
        addRecipe(tm, ingMap.get("Trà Thảo mộc (20 gói)"), 0.05);
        addRecipe(tm, ingMap.get("Syrup Ổi (Chai)"), 0.02);
        addRecipe(tm, ingMap.get("Sunup Ổi (Chai)"), 0.02);
        addRecipe(tm, ingMap.get("Nhãn ngâm (Hộp)"), 3.0/45.0);
        addRecipe(tm, ingMap.get("Cam tươi (kg)"), 1.0/40.0);

        // Hồng trà đào
        Product htd = saveProduct("Hồng trà đào", 55000, 45000, catMap.get("Trà trái cây"), "Miếng đào giòn tan trong nền hồng trà đậm vị.");
        addRecipe(htd, ingMap.get("Hồng trà (200g)"), 0.1);
        addRecipe(htd, ingMap.get("Syrup Đào (Chai)"), 0.03);
        addRecipe(htd, ingMap.get("Đào ngâm (Hộp)"), 3.0/25.0);

        // --- 3. CÀ PHÊ ---
        Product cfd = saveProduct("Cà phê đen", 35000, 30000, catMap.get("Cà phê"), "Cà phê nguyên chất, đậm đà.");
        addRecipe(cfd, ingMap.get("Cà phê hạt (500g)"), 0.025);
        addRecipe(cfd, ingMap.get("Đường cát (kg)"), 0.01);

        Product cfs = saveProduct("Cà phê sữa", 39000, 34000, catMap.get("Cà phê"), "Cà phê pha phin hòa quyện cùng sữa đặc.");
        addRecipe(cfs, ingMap.get("Cà phê hạt (500g)"), 0.025);
        addRecipe(cfs, ingMap.get("Sữa đặc (Lon)"), 0.06);

        Product bx = saveProduct("Bạc xỉu", 45000, 39000, catMap.get("Cà phê"), "Vị sữa béo nhiều hơn vị cà phê.");
        addRecipe(bx, ingMap.get("Cà phê hạt (500g)"), 0.02);
        addRecipe(bx, ingMap.get("Sữa đặc (Lon)"), 0.08);
        addRecipe(bx, ingMap.get("Sữa tươi (Lít)"), 0.08);

        // --- 4. ĐÁ XAY ---
        Product mdx = saveProductOneSize("Matcha đá xay", 65000, catMap.get("Đá xay"), null, "Bột Matcha Nhật Bản xay cùng đá và kem tươi.");
        addRecipe(mdx, ingMap.get("Bột Matcha (kg)"), 0.015);
        addRecipe(mdx, ingMap.get("Bột kem béo (kg)"), 0.02);
        addRecipe(mdx, ingMap.get("Sữa đặc (Lon)"), 0.05);

        Product odx = saveProductOneSize("Oreo kem đá xay", 65000, catMap.get("Đá xay"), null, "Bánh Oreo và kem béo ngậy.");
        addRecipe(odx, ingMap.get("Bột Chocolate (kg)"), 0.01);
        addRecipe(odx, ingMap.get("Sữa đặc (Lon)"), 0.05);
        addRecipe(odx, ingMap.get("Bánh Oreo (12 cái)"), 3.0/12.0);
        addRecipe(odx, ingMap.get("Sữa tươi (Lít)"), 0.1);
        addRecipe(odx, ingMap.get("Bột kem béo (kg)"), 0.02);
        addRecipe(odx, ingMap.get("Syrup Chocolate (Chai)"), 0.02);

        Product scx = saveProductOneSize("Sữa chua xoài đác thơm", 69000, catMap.get("Đá xay"), null, "Vị chua của sữa chua, xoài chín và đác thơm.");
        addRecipe(scx, ingMap.get("Trái xoài (Quả)"), 1.0);
        addRecipe(scx, ingMap.get("Syrup Thơm (Chai)"), 0.02);
        addRecipe(scx, ingMap.get("Đường cát (kg)"), 0.015);
        addRecipe(scx, ingMap.get("Sữa chua (200g)"), 0.5);
        addRecipe(scx, ingMap.get("Đác thơm (1kg)"), 0.03);
        addRecipe(scx, ingMap.get("Trái thơm (Quả)"), 1.0/20.0);

        // --- 5. BÁNH ---
        saveBakery("Bánh mì chà bông phô mai", 35000, catMap.get("Bánh mặn"), "Bánh mì mềm, nhân phô mai và chà bông.");
        saveBakery("Bánh Croissant trứng muối", 45000, catMap.get("Bánh mặn"), "Vỏ giòn tan cùng nhân trứng muối kim sa.");
        saveBakery("Tiramisu", 55000, catMap.get("Bánh ngọt "), "Vị cà phê và phô mai Mascarpone.");
        saveBakery("Cheesecake Chanh Dây", 50000, catMap.get("Bánh ngọt "), "Phô mai béo và chanh dây chua thanh.");
    }

    private void seedVouchers() {
        User user = userRepository.findByUsername("user").orElse(null);
        if (user != null) {
            Voucher v1 = Voucher.builder()
                    .code("FANDB10")
                    .description("Giảm 10% tổng hóa đơn cho hội viên mới")
                    .discountAmount(BigDecimal.valueOf(10))
                    .discountType("PERCENT")
                    .minOrderAmount(BigDecimal.valueOf(100000))
                    .startDate(LocalDateTime.now().minusDays(1))
                    .expiryDate(LocalDateTime.now().plusDays(30))
                    .terms("Áp dụng cho hóa đơn từ 100k. Quét mã QR tại quầy để xác nhận.")
                    .build();
            voucherRepository.save(v1);
            userVoucherRepository.save(UserVoucher.builder().user(user).voucher(v1).used(false).assignedAt(LocalDateTime.now()).build());
        }
    }

    private void seedNews() {
        if (newsRepository.count() == 0) {
            newsRepository.save(News.builder()
                    .title("FANDB TEA & COFFEE - KHAI TRƯƠNG CHI NHÁNH MỚI")
                    .content("Chào mừng chi nhánh thứ 10 của FANDB Bakery tại TP.HCM với nhiều ưu đãi hấp dẫn.")
                    .imageUrl("/uploads/news/tintuc1.png")
                    .build());
            newsRepository.save(News.builder()
                    .title("BỘ SƯU TẬP TRÀ TRÁI CÂY GIẢI NHIỆT MÙA HÈ")
                    .content("Thưởng thức hương vị tươi mát từ trái cây tự nhiên với dòng sản phẩm mới.")
                    .imageUrl("/uploads/news/tintuc2.png")
                    .build());
            newsRepository.save(News.builder()
                    .title("HÀNH TRÌNH TỪ NHỮNG BÚP TRÀ TƯƠI")
                    .content("Tìm hiểu về quy trình sản xuất trà nghiêm ngặt để tạo nên hương vị đặc trưng của FANDB.")
                    .imageUrl("/uploads/news/tintuc3.png")
                    .build());
        }
    }

    private Product saveProduct(String name, int priceL, int priceM, Category cat, String desc) {
        return productRepository.save(Product.builder()
                .name(name)
                .priceL(BigDecimal.valueOf(priceL))
                .priceM(BigDecimal.valueOf(priceM))
                .price(BigDecimal.valueOf(priceM))
                .description(desc)
                .imageUrl(getProductImage(name))
                .category(cat)
                .isRecipeBased(true)
                .build());
    }

    private Product saveProductOneSize(String name, int price, Category cat, String imageUrl, String desc) {
        return productRepository.save(Product.builder()
                .name(name)
                .priceL(BigDecimal.valueOf(price))
                .price(BigDecimal.valueOf(price))
                .description(desc)
                .imageUrl(imageUrl != null ? imageUrl : getProductImage(name))
                .category(cat)
                .isRecipeBased(true)
                .build());
    }

    private void saveBakery(String name, int price, Category cat, String desc) {
        productRepository.save(Product.builder()
                .name(name)
                .price(BigDecimal.valueOf(price))
                .description(desc)
                .imageUrl(getProductImage(name))
                .category(cat)
                .isRecipeBased(false)
                .stock(50) // Gán cứng tồn kho cho bánh
                .build());
    }

    private void seedOrdersToTriggerBestseller() {
        User user = userRepository.findByUsername("user").orElse(null);
        if (user == null) return;

        List<Product> products = productRepository.findAll();
        if (products.size() < 3) return;

        // Tạo 1 đơn hàng ảo với số lượng lớn > 100 để kích hoạt Bestseller cho sản phẩm đầu tiên và thứ hai
        Order order = Order.builder()
                .user(user)
                .orderDate(LocalDateTime.now().minusDays(1))
                .status(Order.OrderStatus.COMPLETED)
                .paymentMethod("COD")
                .totalAmount(BigDecimal.valueOf(10000000))
                .shippingAddress("123 Test Street")
                .build();
        
        orderRepository.save(order);
        // Tạo OrderDetail > 100 số lượng cho 2 sản phẩm đầu
        orderDetailRepository.save(OrderDetail.builder().order(order).product(products.get(0)).quantity(105).price(products.get(0).getPrice()).build());
        orderDetailRepository.save(OrderDetail.builder().order(order).product(products.get(1)).quantity(120).price(products.get(1).getPrice()).build());
        
        // Kích hoạt tính toán cập nhật Bestseller sau khi seed order
        updateBestSellersDirectly();
    }

    private void updateBestSellersDirectly() {
        java.time.LocalDateTime thirtyDaysAgo = java.time.LocalDateTime.now().minusDays(30);
        List<Long> topSellingIds = orderDetailRepository.findTopSellingProductIds(thirtyDaysAgo);
        
        for (Long id : topSellingIds) {
            productRepository.findById(id).ifPresent(p -> {
                p.setBestSeller(true);
                productRepository.save(p);
            });
        }
    }

    private void addRecipe(Product p, Ingredient ing, double qty) {
        if (ing != null) recipeRepository.save(new Recipe(null, p, ing, qty));
    }

    private String getProductImage(String name) {
        if (name == null) return "/uploads/products/default.png";
        
        // 1. Chuyển chữ thường
        String slug = name.toLowerCase();

        // 2. Loại bỏ dấu tiếng Việt
        slug = java.text.Normalizer.normalize(slug, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace("đ", "d").replace("Đ", "d");

        // 3. Xóa khoảng trắng và các ký tự đặc biệt (Giữ lại tên gốc nguyên vẹn)
        slug = slug.replaceAll("[^a-z0-9]", ""); 

        return "/uploads/products/" + slug + ".png";
    }
}

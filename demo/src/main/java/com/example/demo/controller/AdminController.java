package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private CategoryService categoryService;
    @Autowired private ProductService productService;
    @Autowired private OrderService orderService;
    @Autowired private UserService userService;
    @Autowired private NewsService newsService;
    @Autowired private InventoryService inventoryService;
    @Autowired private NotificationService notificationService;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private ReviewService reviewService;
    @Autowired private com.example.demo.repository.RecipeRepository recipeRepository;
    @Autowired private com.example.demo.repository.IngredientRepository ingredientRepository;
    @Autowired private com.example.demo.repository.VoucherRepository voucherRepository;
    @Autowired private com.example.demo.repository.UserVoucherRepository userVoucherRepository;
    @Autowired private com.example.demo.repository.UserRepository userRepository;
    @Autowired private com.example.demo.repository.PromotionRepository promotionRepository;
    @Autowired private com.example.demo.service.SystemSettingService systemSettingService;
    @Autowired private com.example.demo.repository.SystemSettingRepository systemSettingRepository;
    @Autowired private com.example.demo.repository.InventoryLogRepository inventoryLogRepository;
    @Autowired private RevenueService revenueService;

    @GetMapping
    public String dashboard(Model model) {
        List<Order> allOrders = orderService.getAllOrders();
        
        BigDecimal todayRevenue = allOrders.stream()
                .filter(o -> o.getOrderDate() != null && o.getOrderDate().toLocalDate().isEqual(LocalDateTime.now().toLocalDate()))
                .filter(o -> o.getStatus() == Order.OrderStatus.COMPLETED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthRevenue = allOrders.stream()
                .filter(o -> o.getOrderDate() != null 
                             && o.getOrderDate().getMonth() == LocalDateTime.now().getMonth() 
                             && o.getOrderDate().getYear() == LocalDateTime.now().getYear())
                .filter(o -> o.getStatus() == Order.OrderStatus.COMPLETED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingOrders = allOrders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.PENDING)
                .count();

        model.addAttribute("categoryCount", categoryService.getAllCategories().size());
        model.addAttribute("productCount", productService.getAllProducts().size());
        model.addAttribute("orderCount", allOrders.size());
        model.addAttribute("userCount", userService.count());
        model.addAttribute("todayRevenue", todayRevenue);
        model.addAttribute("monthRevenue", monthRevenue);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("lowStockIngredients", inventoryService.getLowStockIngredients());
        
        return "admin/index";
    }

    // Category Management
    @GetMapping("/categories")
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/categories";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@ModelAttribute Category category, @RequestParam(required = false) Long parentId, RedirectAttributes ra) {
        if (parentId != null) {
            Category parent = categoryService.getCategoryById(parentId);
            category.setParentCategory(parent);
        }
        categoryService.saveCategory(category);
        ra.addFlashAttribute("successMessage", "Lưu danh mục thành công");
        return "redirect:/admin/categories";
    }

    @GetMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes ra) {
        categoryService.deleteCategory(id);
        ra.addFlashAttribute("successMessage", "Xóa danh mục thành công");
        return "redirect:/admin/categories";
    }

    // Product Management
    @GetMapping("/products")
    public String listProducts(Model model) {
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("promotions", promotionRepository.findAll());
        if (!model.containsAttribute("product")) {
            model.addAttribute("product", new Product());
        }
        return "admin/products";
    }

    @PostMapping("/products/save")
    public String saveProduct(@ModelAttribute("product") @Valid Product product, 
                              BindingResult bindingResult, 
                              @RequestParam Long categoryId,
                              @RequestParam(required = false) Long promotionId,
                              @RequestParam("imageFile") MultipartFile imageFile,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("products", productService.getAllProducts());
            model.addAttribute("categories", categoryService.getAllCategories());
            model.addAttribute("promotions", promotionRepository.findAll());
            return "admin/products";
        }

        Category category = categoryService.getCategoryById(categoryId);
        product.setCategory(category);
        
        if (product.getPrice() == null) {
            product.setPrice(product.getPriceM());
        }

        if (promotionId != null) {
            product.setPromotion(promotionRepository.findById(promotionId).orElse(null));
        } else {
            product.setPromotion(null);
        }

        if (!imageFile.isEmpty()) {
            String fileName = fileStorageService.storeFile(imageFile);
            product.setImageUrl("/uploads/" + fileName);
        }

        productService.saveProduct(product);
        redirectAttributes.addFlashAttribute("successMessage", "Lưu sản phẩm thành công");
        return "redirect:/admin/products";
    }

    @GetMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productService.deleteProduct(id);
        redirectAttributes.addFlashAttribute("successMessage", "Xóa sản phẩm thành công");
        return "redirect:/admin/products";
    }

    // Order Management
    @GetMapping("/orders")
    public String listOrders(Model model) {
        model.addAttribute("orders", orderService.getAllOrders());
        return "admin/orders";
    }

    @PostMapping("/orders/status")
    public String updateOrderStatus(@RequestParam Long orderId, @RequestParam Order.OrderStatus status, RedirectAttributes redirectAttributes) {
        orderService.updateOrderStatus(orderId, status);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn hàng thành công");
        return "redirect:/admin/orders";
    }

    @GetMapping("/orders/print/{id}")
    public String printInvoice(@PathVariable Long id, Model model) {
        Order order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        return "admin/invoice-print";
    }

    @GetMapping("/orders/details-fragment/{id}")
    public String getOrderDetailsFragment(@PathVariable Long id, Model model) {
        Order order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        return "admin/orders :: orderDetailsContent";
    }

    // User Management
    @GetMapping("/users")
    public String listUsers(@RequestParam(required = false) String query, Model model) {
        List<User> users;
        if (query != null && !query.isEmpty()) {
            String lowerQuery = query.toLowerCase();
            users = userService.findAll().stream()
                .filter(u -> (u.getFullName() != null && u.getFullName().toLowerCase().contains(lowerQuery)) ||
                             (u.getUsername() != null && u.getUsername().toLowerCase().contains(lowerQuery)) ||
                             (u.getPhone() != null && u.getPhone().contains(query)) ||
                             (u.getEmail() != null && u.getEmail().toLowerCase().contains(lowerQuery)))
                .collect(Collectors.toList());
        } else {
            users = userService.findAll();
        }
        model.addAttribute("users", users);
        model.addAttribute("query", query);
        return "admin/users";
    }

    @PostMapping("/users/toggle-status/{id}")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes ra) {
        userService.toggleUserStatus(id);
        ra.addFlashAttribute("successMessage", "Cập nhật trạng thái người dùng thành công");
        return "redirect:/admin/users";
    }

    // Review Management
    @GetMapping("/reviews")
    public String listReviews(Model model) {
        model.addAttribute("reviews", reviewService.findAll());
        return "admin/reviews";
    }

    @PostMapping("/reviews/status")
    public String updateReviewStatus(@RequestParam Long id, @RequestParam String status, RedirectAttributes ra) {
        reviewService.updateStatus(id, status);
        ra.addFlashAttribute("successMessage", "Cập nhật trạng thái đánh giá thành công");
        return "redirect:/admin/reviews";
    }

    @GetMapping("/reviews/delete/{id}")
    public String deleteReview(@PathVariable Long id, RedirectAttributes ra) {
        reviewService.deleteReview(id);
        ra.addFlashAttribute("successMessage", "Xóa đánh giá thành công");
        return "redirect:/admin/reviews";
    }

    // News Management
    @GetMapping("/news")
    public String listNews(Model model) {
        model.addAttribute("newsList", newsService.getAllNews());
        return "admin/news";
    }

    @PostMapping("/news/save")
    public String saveNews(@ModelAttribute News news, RedirectAttributes redirectAttributes) {
        News savedNews = newsService.saveNews(news);
        notificationService.sendGlobalNotification(
            "Tin mới: " + savedNews.getTitle(),
            "Xem ngay tin tức và khuyến mãi mới nhất từ FANDB Bakery.",
            "PROMOTION", "/news/" + savedNews.getId()
        );
        redirectAttributes.addFlashAttribute("successMessage", "Lưu tin tức thành công");
        return "redirect:/admin/news";
    }

    @GetMapping("/news/delete/{id}")
    public String deleteNews(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        newsService.deleteNews(id);
        redirectAttributes.addFlashAttribute("successMessage", "Xóa tin tức thành công");
        return "redirect:/admin/news";
    }

    // Promotion Management
    @GetMapping("/promotions")
    public String listPromotions(Model model) {
        model.addAttribute("promotions", promotionRepository.findAll());
        return "admin/promotions";
    }

    @PostMapping("/promotions/save")
    public String savePromotion(@ModelAttribute Promotion promotion, RedirectAttributes ra) {
        promotionRepository.save(promotion);
        ra.addFlashAttribute("successMessage", "Đã lưu chương trình khuyến mãi");
        return "redirect:/admin/promotions";
    }

    @PostMapping("/promotions/toggle/{id}")
    public String togglePromotion(@PathVariable Long id, RedirectAttributes ra) {
        Promotion p = promotionRepository.findById(id).orElseThrow();
        p.setIsActive(!p.getIsActive());
        promotionRepository.save(p);
        ra.addFlashAttribute("successMessage", "Đã cập nhật trạng thái khuyến mãi");
        return "redirect:/admin/promotions";
    }

    @GetMapping("/promotions/delete/{id}")
    public String deletePromotion(@PathVariable Long id, RedirectAttributes ra) {
        promotionRepository.deleteById(id);
        ra.addFlashAttribute("successMessage", "Đã xóa chương trình khuyến mãi");
        return "redirect:/admin/promotions";
    }

    // Inventory View
    @GetMapping("/inventory")
    public String viewInventory(Model model) {
        List<Ingredient> ingredients = inventoryService.getAllIngredients();
        BigDecimal totalInventoryValue = ingredients.stream()
            .map(i -> i.getCostPerUnit().multiply(BigDecimal.valueOf(i.getStockQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        model.addAttribute("ingredients", ingredients);
        model.addAttribute("inventoryLogs", inventoryLogRepository.findAllByOrderByCreatedAtDesc());
        model.addAttribute("totalValue", totalInventoryValue);
        model.addAttribute("lowStockCount", inventoryService.getLowStockIngredients().size());
        return "admin/inventory";
    }

    @PostMapping("/inventory/restock")
    public String restockIngredient(@RequestParam Long id, 
                                   @RequestParam Double quantity,
                                   @RequestParam BigDecimal price,
                                   @RequestParam String supplier,
                                   @RequestParam(required = false) String note,
                                   Authentication auth,
                                   RedirectAttributes ra) {
        User performer = userService.findByUsername(auth.getName());
        inventoryService.restock(id, quantity, price, supplier, performer, note);
        ra.addFlashAttribute("successMessage", "Đã nhập kho thành công");
        return "redirect:/admin/inventory";
    }

    // Voucher Management
    @GetMapping("/vouchers")
    public String listVouchers(Model model) {
        model.addAttribute("vouchers", voucherRepository.findAll());
        model.addAttribute("users", userService.findAll());
        return "admin/vouchers";
    }

    @PostMapping("/vouchers/save")
    public String saveVoucher(@ModelAttribute Voucher voucher, RedirectAttributes ra) {
        voucherRepository.save(voucher);
        ra.addFlashAttribute("successMessage", "Lưu Voucher thành công");
        return "redirect:/admin/vouchers";
    }

    @PostMapping("/vouchers/assign")
    public String assignVoucher(@RequestParam Long voucherId, @RequestParam(required = false) Long userId, RedirectAttributes ra) {
        Voucher voucher = voucherRepository.findById(voucherId).orElseThrow();
        if (userId != null) {
            User user = userRepository.findById(userId).orElseThrow();
            userVoucherRepository.save(UserVoucher.builder()
                .user(user)
                .voucher(voucher)
                .assignedAt(LocalDateTime.now())
                .used(false)
                .build());
            notificationService.sendNotification(user, "Bạn nhận được Voucher mới!", "Mã Voucher: " + voucher.getCode() + ". Hãy sử dụng ngay!", "SYSTEM", "/user/vouchers");
        } else {
            List<User> allUsers = userService.findAll();
            for (User u : allUsers) {
                userVoucherRepository.save(UserVoucher.builder()
                    .user(u)
                    .voucher(voucher)
                    .assignedAt(LocalDateTime.now())
                    .used(false)
                    .build());
            }
            notificationService.sendGlobalNotification("Quà tặng: Voucher toàn hệ thống!", "FANDB gửi tặng tất cả khách hàng Voucher mới. Kiểm tra ngay trong mục Voucher!", "SYSTEM", "/user/vouchers");
        }
        ra.addFlashAttribute("successMessage", "Đã phát hành Voucher thành công");
        return "redirect:/admin/vouchers";
    }

    // Notification Management
    @GetMapping("/notifications")
    public String listNotifications(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/notifications";
    }

    @PostMapping("/notifications/send")
    public String sendNotification(@RequestParam String title, 
                                   @RequestParam String content,
                                   @RequestParam String type,
                                   @RequestParam(required = false) String link,
                                   @RequestParam(required = false) Long userId,
                                   RedirectAttributes ra) {
        if (userId != null) {
            User user = userRepository.findById(userId).orElseThrow();
            notificationService.sendNotification(user, title, content, type, link);
            ra.addFlashAttribute("successMessage", "Đã gửi thông báo cá nhân thành công");
        } else {
            notificationService.sendGlobalNotification(title, content, type, link);
            ra.addFlashAttribute("successMessage", "Đã gửi thông báo toàn hệ thống thành công");
        }
        return "redirect:/admin/notifications";
    }

    // Settings Management
    @GetMapping("/settings")
    public String viewSettings(Model model) {
        List<SystemSetting> allSettings = systemSettingRepository.findAll();
        java.util.Map<String, String> settingsMap = allSettings.stream()
            .collect(Collectors.toMap(SystemSetting::getKey, SystemSetting::getValue, (v1, v2) -> v1));
        
        model.addAttribute("settings", settingsMap);
        return "admin/settings";
    }

    @PostMapping("/settings/save")
    public String saveSettings(@RequestParam java.util.Map<String, String> allParams, RedirectAttributes ra) {
        allParams.forEach((key, value) -> {
            if (!key.equals("_csrf")) {
                systemSettingService.updateSetting(key, value, null, "GENERAL");
            }
        });
        
        if (!allParams.containsKey("maintenance_mode")) {
            systemSettingService.updateSetting("maintenance_mode", "false", null, "GENERAL");
        } else {
            systemSettingService.updateSetting("maintenance_mode", "true", null, "GENERAL");
        }
        
        if (!allParams.containsKey("store_status")) {
            systemSettingService.updateSetting("store_status", "CLOSED", null, "SYSTEM");
        } else {
            systemSettingService.updateSetting("store_status", "OPEN", null, "SYSTEM");
        }

        ra.addFlashAttribute("successMessage", "Đã lưu tất cả cấu hình hệ thống");
        return "redirect:/admin/settings";
    }

    // Recipe Management
    @GetMapping("/recipes/{productId}")
    public String viewRecipe(@PathVariable Long productId, Model model) {
        Product product = productService.getProductById(productId);
        List<Recipe> recipes = recipeRepository.findByProduct(product);
        
        BigDecimal totalCost = recipes.stream()
            .map(r -> r.getIngredient().getCostPerUnit().multiply(BigDecimal.valueOf(r.getQuantityRequired())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("product", product);
        model.addAttribute("recipes", recipes);
        model.addAttribute("ingredients", inventoryService.getAllIngredients());
        model.addAttribute("totalCost", totalCost);
        return "admin/recipes";
    }

    @PostMapping("/recipes/save")
    public String saveRecipe(@RequestParam Long productId, 
                             @RequestParam Long ingredientId, 
                             @RequestParam Double quantity,
                             RedirectAttributes ra) {
        Product product = productService.getProductById(productId);
        Ingredient ingredient = ingredientRepository.findById(ingredientId).orElseThrow();
        
        Recipe recipe = Recipe.builder()
            .product(product)
            .ingredient(ingredient)
            .quantityRequired(quantity)
            .build();
            
        recipeRepository.save(recipe);
        ra.addFlashAttribute("successMessage", "Đã cập nhật công thức");
        return "redirect:/admin/recipes/" + productId;
    }

    // Revenue Management
    @GetMapping("/revenue")
    public String viewRevenue(@RequestParam(required = false) String date, Model model) {
        java.time.LocalDate reportDate = (date != null && !date.isEmpty()) ? 
            java.time.LocalDate.parse(date) : java.time.LocalDate.now();
            
        com.example.demo.dto.RevenueReportDTO report = revenueService.generateDailyReport(reportDate);
        model.addAttribute("report", report);
        model.addAttribute("selectedDate", reportDate);
        return "admin/revenue";
    }

    @GetMapping("/revenue/print")
    public String printRevenue(@RequestParam(required = false) String date, Model model) {
        java.time.LocalDate reportDate = (date != null && !date.isEmpty()) ? 
            java.time.LocalDate.parse(date) : java.time.LocalDate.now();
            
        com.example.demo.dto.RevenueReportDTO report = revenueService.generateDailyReport(reportDate);
        model.addAttribute("report", report);
        return "admin/revenue-print";
    }
}

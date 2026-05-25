package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.service.InventoryService;
import com.example.demo.service.OrderService;
import com.example.demo.service.ProductService;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private UserService userService;

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            String email = oauth2User.getAttribute("email");
            return userService.findByEmail(email);
        }
        return userService.findByUsername(authentication.getName());
    }

    @SuppressWarnings("unchecked")
    private List<CartItem> getCartFromSession(HttpSession session) {
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart_items");
        if (cart == null) {
            cart = new ArrayList<>();
            session.setAttribute("cart_items", cart);
        }
        return cart;
    }

    @GetMapping
    public String viewCart(HttpSession session, Model model) {
        List<CartItem> cartItems = getCartFromSession(session);
        BigDecimal subtotal = calculateSubtotal(cartItems);

        BigDecimal shippingFee = BigDecimal.valueOf(15000); 
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal vatRate = BigDecimal.valueOf(0.1); 
        BigDecimal vatAmount = subtotal.multiply(vatRate);
        BigDecimal total = subtotal.add(shippingFee).add(vatAmount).subtract(discount);

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("discount", discount);
        model.addAttribute("vatAmount", vatAmount);
        model.addAttribute("total", total);
        
        return "cart";
    }

    private BigDecimal calculateSubtotal(List<CartItem> cartItems) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            subtotal = subtotal.add(item.getSubtotal());
        }
        return subtotal;
    }

    @Autowired
    private com.example.demo.repository.UserVoucherRepository userVoucherRepository;

    @Autowired
    private com.example.demo.repository.VoucherRepository voucherRepository;

    @PostMapping("/prepare-checkout")
    public String prepareCheckout(@RequestParam String fullName,
                                 @RequestParam String phone,
                                 @RequestParam(required = false) String shippingAddress,
                                 @RequestParam String paymentMethod,
                                 HttpSession session) {
        session.setAttribute("temp_fullName", fullName);
        session.setAttribute("temp_phone", phone);
        session.setAttribute("temp_address", shippingAddress);
        session.setAttribute("temp_paymentMethod", paymentMethod);
        return "redirect:/cart/checkout";
    }

    @GetMapping("/checkout")
    public String checkoutPage(HttpSession session, Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return "redirect:/login";
        }
        User user = getAuthenticatedUser(authentication);
        
        List<CartItem> cartItems = getCartFromSession(session);
        if (cartItems.isEmpty()) {
            return "redirect:/cart";
        }

        BigDecimal subtotal = calculateSubtotal(cartItems);
        
        // Lấy phương thức nhận hàng (mặc định là DELIVERY)
        String deliveryMethod = (String) session.getAttribute("temp_deliveryMethod");
        if (deliveryMethod == null) deliveryMethod = "DELIVERY";
        
        BigDecimal shippingFee = deliveryMethod.equals("STORE") ? BigDecimal.ZERO : BigDecimal.valueOf(15000);
        BigDecimal vatAmount = subtotal.multiply(BigDecimal.valueOf(0.1));
        
        BigDecimal discount = BigDecimal.ZERO;
        Voucher appliedVoucher = (Voucher) session.getAttribute("appliedVoucher");
        if (appliedVoucher != null) {
            if (appliedVoucher.getDiscountType().equals("PERCENT")) {
                discount = subtotal.multiply(appliedVoucher.getDiscountAmount()).divide(BigDecimal.valueOf(100));
                if (appliedVoucher.getMaxDiscount() != null && discount.compareTo(appliedVoucher.getMaxDiscount()) > 0) {
                    discount = appliedVoucher.getMaxDiscount();
                }
            } else {
                discount = appliedVoucher.getDiscountAmount();
            }
        }

        // Ưu đãi đổi điểm (100 điểm = 1 ly nước miễn phí)
        boolean isRedeeming = Boolean.TRUE.equals(session.getAttribute("redeemPoints"));
        BigDecimal pointsDiscount = BigDecimal.ZERO;
        if (isRedeeming && user != null && user.getPoints() >= 100) {
            for (CartItem item : cartItems) {
                Product p = productService.getProductById(item.getProductId());
                if (p.getIsRecipeBased() && "M".equals(item.getSize())) {
                    pointsDiscount = item.getPrice();
                    break;
                }
            }
        }
        discount = discount.add(pointsDiscount);

        BigDecimal total = subtotal.add(shippingFee).add(vatAmount).subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

        // Ưu tiên lấy từ session
        String fullName = (String) session.getAttribute("temp_fullName");
        String phone = (String) session.getAttribute("temp_phone");
        String address = (String) session.getAttribute("temp_address");
        String paymentMethod = (String) session.getAttribute("temp_paymentMethod");

        if (fullName == null && user != null) fullName = user.getFullName() != null ? user.getFullName() : user.getUsername();
        if (phone == null && user != null) phone = user.getPhone();
        if (address == null && user != null) address = user.getAddressDetail();
        if (paymentMethod == null) paymentMethod = "COD";

        model.addAttribute("user", user);
        model.addAttribute("fullName", fullName);
        model.addAttribute("phone", phone);
        model.addAttribute("address", address);
        model.addAttribute("paymentMethod", paymentMethod);
        model.addAttribute("deliveryMethod", deliveryMethod);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("vatAmount", vatAmount);
        model.addAttribute("discount", discount);
        model.addAttribute("total", total);
        model.addAttribute("appliedVoucher", appliedVoucher);
        model.addAttribute("userVouchers", user != null ? userVoucherRepository.findByUserAndUsedFalse(user) : new java.util.ArrayList<>());
        
        return "checkout";
    }

    @PostMapping("/apply-voucher")
    public String applyVoucher(@RequestParam(required = false) String voucherCode,
                              @RequestParam(required = false) String fullName,
                              @RequestParam(required = false) String phone,
                              @RequestParam(required = false) String shippingAddress,
                              @RequestParam(required = false) String paymentMethod,
                              @RequestParam(required = false) String deliveryMethod,
                              HttpSession session,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        // Lưu thông tin nhập liệu vào session
        if (fullName != null) session.setAttribute("temp_fullName", fullName);
        if (phone != null) session.setAttribute("temp_phone", phone);
        if (shippingAddress != null) session.setAttribute("temp_address", shippingAddress);
        if (paymentMethod != null) session.setAttribute("temp_paymentMethod", paymentMethod);
        if (deliveryMethod != null) session.setAttribute("temp_deliveryMethod", deliveryMethod);

        if (voucherCode == null || voucherCode.isEmpty()) {
            session.removeAttribute("appliedVoucher");
            return "redirect:/cart/checkout";
        }

        User user = getAuthenticatedUser(authentication);
        List<UserVoucher> userVouchers = userVoucherRepository.findByUserAndUsedFalse(user);
        
        java.util.Optional<UserVoucher> matchingVoucher = userVouchers.stream()
                .filter(uv -> uv.getVoucher().getCode().equals(voucherCode))
                .findFirst();

        if (matchingVoucher.isPresent()) {
            Voucher v = matchingVoucher.get().getVoucher();
            BigDecimal subtotal = calculateSubtotal(getCartFromSession(session));
            
            if (v.getMinOrderAmount() != null && subtotal.compareTo(v.getMinOrderAmount()) < 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "Đơn hàng tối thiểu " + v.getMinOrderAmount() + "đ để sử dụng voucher này.");
            } else {
                session.setAttribute("appliedVoucher", v);
                redirectAttributes.addFlashAttribute("successMessage", "Đã áp dụng mã " + voucherCode);
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Mã giảm giá không hợp lệ hoặc đã sử dụng.");
        }
        
        return "redirect:/cart/checkout";
    }

    @PostMapping("/checkout")
    public String processCheckout(@RequestParam(required = false) String shippingAddress,
                                 @RequestParam String paymentMethod,
                                 @RequestParam String deliveryMethod,
                                 HttpSession session,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(authentication);

        List<CartItem> cartItems = getCartFromSession(session);
        if (cartItems.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng của bạn đang trống.");
            return "redirect:/cart";
        }

        // Kiểm tra tồn kho lần cuối trước khi tạo đơn hàng
        for (CartItem item : cartItems) {
            Product product = productService.getProductById(item.getProductId());
            int availableStock = inventoryService.getCalculatedStock(product);
            if (item.getQuantity() > availableStock) {
                redirectAttributes.addFlashAttribute("errorMessage", "Sản phẩm " + product.getName() + " đã hết hàng hoặc không đủ số lượng.");
                return "redirect:/cart";
            }
        }

        BigDecimal subtotal = calculateSubtotal(cartItems);
        BigDecimal shippingFee = deliveryMethod.equals("STORE") ? BigDecimal.ZERO : BigDecimal.valueOf(15000);
        BigDecimal vatAmount = subtotal.multiply(BigDecimal.valueOf(0.1));
        
        BigDecimal discount = BigDecimal.ZERO;
        Voucher appliedVoucher = (Voucher) session.getAttribute("appliedVoucher");
        if (appliedVoucher != null) {
            if (appliedVoucher.getDiscountType().equals("PERCENT")) {
                discount = subtotal.multiply(appliedVoucher.getDiscountAmount()).divide(BigDecimal.valueOf(100));
                if (appliedVoucher.getMaxDiscount() != null && discount.compareTo(appliedVoucher.getMaxDiscount()) > 0) {
                    discount = appliedVoucher.getMaxDiscount();
                }
            } else {
                discount = appliedVoucher.getDiscountAmount();
            }
        }

        // Ưu đãi đổi điểm (100 điểm = 1 ly nước miễn phí)
        boolean isRedeeming = Boolean.TRUE.equals(session.getAttribute("redeemPoints"));
        BigDecimal pointsDiscount = BigDecimal.ZERO;
        if (isRedeeming && user != null && user.getPoints() >= 100) {
            for (CartItem item : cartItems) {
                Product p = productService.getProductById(item.getProductId());
                if (p.getIsRecipeBased() && "M".equals(item.getSize())) {
                    pointsDiscount = item.getPrice();
                    break;
                }
            }
        }
        discount = discount.add(pointsDiscount);

        BigDecimal total = subtotal.add(shippingFee).add(vatAmount).subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

        Order order = Order.builder()
                .user(user)
                .totalAmount(total)
                .shippingAddress(deliveryMethod.equals("STORE") ? "Nhận tại cửa hàng" : shippingAddress)
                .paymentMethod(paymentMethod)
                .deliveryMethod(deliveryMethod)
                .shippingFee(shippingFee)
                .voucherCode(appliedVoucher != null ? appliedVoucher.getCode() : null)
                .discountAmount(discount)
                .status(Order.OrderStatus.PENDING)
                .build();

        List<OrderDetail> details = cartItems.stream().map(item -> {
            Product product = productService.getProductById(item.getProductId());
            return OrderDetail.builder()
                    .product(product)
                    .quantity(item.getQuantity())
                    .price(item.getPrice())
                    .size(item.getSize())
                    .note(item.getNote())
                    .build();
        }).collect(Collectors.toList());

        Order savedOrder = orderService.createOrder(order, details);

        // Khấu trừ điểm nếu có đổi quà
        if (isRedeeming && user != null) {
            userService.deductPoints(user, 100, "Đổi 100 điểm lấy 1 ly nước miễn phí (Đơn hàng #" + savedOrder.getId() + ")");
            session.removeAttribute("redeemPoints");
        }

        // Mark voucher as used
        if (appliedVoucher != null) {
            List<UserVoucher> uvs = userVoucherRepository.findByUserAndUsedFalse(user);
            uvs.stream()
               .filter(uv -> uv.getVoucher().getCode().equals(appliedVoucher.getCode()))
               .findFirst()
               .ifPresent(uv -> {
                   uv.setUsed(true);
                   userVoucherRepository.save(uv);
               });
        }

        session.removeAttribute("appliedVoucher");
        session.removeAttribute("temp_fullName");
        session.removeAttribute("temp_phone");
        session.removeAttribute("temp_address");
        session.removeAttribute("temp_paymentMethod");
        session.removeAttribute("temp_deliveryMethod");
        session.setAttribute("cart_items", new ArrayList<CartItem>());
        
        // Điều hướng dựa trên phương thức thanh toán
        if ("MOMO".equals(paymentMethod)) {
            return "redirect:/cart/payment/momo/" + savedOrder.getId();
        } else if ("BANK_TRANSFER".equals(paymentMethod)) {
            return "redirect:/cart/payment/bank/" + savedOrder.getId();
        }
        
        return "redirect:/cart/order-success/" + savedOrder.getId();
    }

    @GetMapping("/payment/momo/{id}")
    public String paymentMomo(@PathVariable Long id, Model model) {
        Order order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        return "payment-momo";
    }

    @GetMapping("/payment/bank/{id}")
    public String paymentBank(@PathVariable Long id, Model model) {
        Order order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        return "payment-bank";
    }

    @GetMapping("/order-success/{id}")
    public String orderSuccess(@PathVariable Long id, Model model) {
        Order order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        return "order-success";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam Long productId,
                           @RequestParam(defaultValue = "1") Integer quantity,
                           @RequestParam(defaultValue = "M") String size,
                           @RequestParam(required = false) String note,
                           HttpSession session,
                           @RequestHeader(value = "Referer", required = false) String referer,
                           RedirectAttributes redirectAttributes) {
        
        Product product = productService.getProductById(productId);
        if (product != null) {
            int availableStock = inventoryService.getCalculatedStock(product);
            List<CartItem> cart = getCartFromSession(session);
            
            // Tính tổng số lượng hiện có trong giỏ hàng cho sản phẩm này
            int currentInCart = cart.stream()
                    .filter(item -> item.getProductId().equals(productId))
                    .mapToInt(CartItem::getQuantity)
                    .sum();
            
            if (currentInCart + quantity > availableStock) {
                redirectAttributes.addFlashAttribute("errorMessage", "Rất tiếc, sản phẩm này chỉ còn " + availableStock + " sản phẩm. Bạn đang có " + currentInCart + " trong giỏ hàng.");
                return (referer != null) ? "redirect:" + referer : "redirect:/menu";
            }

            // Tính giá dựa trên size và áp dụng khuyến mãi nếu có
            BigDecimal basePrice = "L".equals(size) ? product.getPriceL() : product.getPriceM();
            if (basePrice == null) basePrice = product.getPriceM() != null ? product.getPriceM() : product.getPrice();
            
            BigDecimal finalPrice = basePrice;
            if (product.getPromotion() != null && product.getPromotion().isCurrentlyActive()) {
                if (product.getPromotion().getDiscountType() == Promotion.DiscountType.PERCENT) {
                    BigDecimal discount = basePrice.multiply(product.getPromotion().getDiscountValue()).divide(BigDecimal.valueOf(100));
                    finalPrice = basePrice.subtract(discount);
                } else {
                    finalPrice = basePrice.subtract(product.getPromotion().getDiscountValue()).max(BigDecimal.ZERO);
                }
            }
            
            // Logic gộp sản phẩm: Cùng ID, cùng Size VÀ cùng Ghi chú thì mới gộp
            boolean merged = false;
            String normalizedNote = (note != null) ? note.trim() : "";
            
            for (CartItem item : cart) {
                String itemNote = (item.getNote() != null) ? item.getNote().trim() : "";
                if (item.getProductId().equals(productId) && item.getSize().equals(size) && itemNote.equals(normalizedNote)) {
                    item.setQuantity(item.getQuantity() + quantity);
                    merged = true;
                    break;
                }
            }

            if (!merged) {
                CartItem newItem = CartItem.builder()
                        .productId(productId)
                        .productName(product.getName())
                        .imageUrl(product.getImageUrl())
                        .quantity(quantity)
                        .price(finalPrice)
                        .size(size)
                        .note(note)
                        .build();
                cart.add(newItem);
            }
            
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm " + product.getName() + " vào giỏ hàng.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm.");
        }
        
        return (referer != null) ? "redirect:" + referer : "redirect:/menu";
    }

    @GetMapping("/remove/{index}")
    public String removeFromCart(@PathVariable int index, HttpSession session, RedirectAttributes redirectAttributes) {
        List<CartItem> cart = getCartFromSession(session);
        if (index >= 0 && index < cart.size()) {
            String name = cart.get(index).getProductName();
            cart.remove(index);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa " + name + " khỏi giỏ hàng.");
        }
        return "redirect:/cart";
    }

    @PostMapping("/update")
    public String updateCart(@RequestParam(required = false) Integer index,
                           @RequestParam(required = false) Long productId, 
                           @RequestParam(required = false) String size,
                           @RequestParam Integer quantity, 
                           HttpSession session, 
                           RedirectAttributes redirectAttributes) {
        List<CartItem> cart = getCartFromSession(session);
        
        // Ưu tiên cập nhật theo index để chính xác tuyệt đối
        if (index != null && index >= 0 && index < cart.size()) {
            CartItem item = cart.get(index);
            Product product = productService.getProductById(item.getProductId());
            int availableStock = inventoryService.getCalculatedStock(product);

            if (quantity <= 0) {
                cart.remove(item);
                redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ hàng.");
            } else {
                if (quantity > availableStock) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Số lượng yêu cầu vượt quá tồn kho hiện có.");
                    item.setQuantity(availableStock);
                } else {
                    item.setQuantity(quantity);
                    redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật giỏ hàng.");
                }
            }
        } else if (productId != null && size != null) {
            // Fallback logic cũ nếu không có index
            Product product = productService.getProductById(productId);
            int availableStock = inventoryService.getCalculatedStock(product);
            for (CartItem item : cart) {
                if (item.getProductId().equals(productId) && item.getSize().equals(size)) {
                    if (quantity <= 0) {
                        cart.remove(item);
                    } else {
                        item.setQuantity(Math.min(quantity, availableStock));
                    }
                    break;
                }
            }
        }
        return "redirect:/cart";
    }

    @PostMapping("/redeem-points")
    public String redeemPoints(@RequestParam(defaultValue = "false") boolean redeem, HttpSession session, RedirectAttributes ra) {
        session.setAttribute("redeemPoints", redeem);
        if (redeem) {
            ra.addFlashAttribute("successMessage", "Đã áp dụng đổi 100 điểm lấy 1 ly nước miễn phí.");
        } else {
            ra.addFlashAttribute("successMessage", "Đã hủy đổi điểm.");
        }
        return "redirect:/cart/checkout";
    }
}

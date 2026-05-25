package com.example.demo.controller;

import com.example.demo.model.Category;
import com.example.demo.model.News;
import com.example.demo.model.Product;
import com.example.demo.service.CategoryService;
import com.example.demo.service.NewsService;
import com.example.demo.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class MainController {
    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private NewsService newsService;

    @Autowired
    private com.example.demo.service.ReviewService reviewService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("bestSellers", productService.getBestSellers());
        model.addAttribute("newsList", newsService.getAllNews());
        return "home";
    }

    @GetMapping("/menu")
    public String menu(Model model) {
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "menu";
    }

    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id);
        model.addAttribute("product", product);
        model.addAttribute("reviews", reviewService.findByProduct(product));
        return "product-detail";
    }

    @GetMapping("/category/{id}")
    public String categoryFilter(@PathVariable Long id, Model model) {
        Category category = categoryService.getCategoryById(id);
        List<Product> products = productService.getProductsByCategory(category);
        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("selectedCategory", category);
        return "menu";
    }

    @GetMapping("/search")
    public String search(@RequestParam String query, Model model) {
        List<Product> products = productService.searchProducts(query);
        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("query", query);
        return "menu";
    }

    @GetMapping("/ordering-policy")
    public String orderingPolicy() {
        return "ordering-policy";
    }

    @GetMapping("/privacy-policy")
    public String privacyPolicy() {
        return "privacy-policy";
    }

    @GetMapping("/terms-of-service")
    public String termsOfService() {
        return "terms-of-service";
    }

    @Autowired
    private com.example.demo.repository.UserVoucherRepository userVoucherRepository;

    @Autowired
    private com.example.demo.service.UserService userService;

    @GetMapping("/promotions")
    public String promotions(org.springframework.security.core.Authentication authentication, Model model) {
        model.addAttribute("newsList", newsService.getAllNews());
        if (authentication != null && authentication.isAuthenticated()) {
            com.example.demo.model.User user;
            if (authentication instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) {
                String email = ((org.springframework.security.oauth2.core.user.OAuth2User) authentication.getPrincipal()).getAttribute("email");
                user = userService.findByEmail(email);
            } else {
                user = userService.findByUsername(authentication.getName());
            }
            if (user != null) {
                model.addAttribute("userVouchers", userVoucherRepository.findByUserAndUsedFalse(user));
            }
        }
        return "promotions";
    }

    @GetMapping("/news/{id}")
    public String newsDetail(@PathVariable Long id, Model model) {
        News news = newsService.getNewsById(id);
        model.addAttribute("news", news);
        return "news-detail";
    }
}

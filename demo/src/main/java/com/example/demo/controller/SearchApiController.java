package com.example.demo.controller;

import com.example.demo.model.Product;
import com.example.demo.model.User;
import com.example.demo.service.ProductService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class SearchApiController {

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @GetMapping("/member-lookup")
    public ResponseEntity<?> lookupMember(@RequestParam String key) {
        return userService.findByUsernameOrPhone(key)
            .map(user -> {
                Map<String, Object> map = new HashMap<>();
                map.put("fullName", user.getFullName() != null ? user.getFullName() : user.getUsername());
                map.put("username", user.getUsername());
                map.put("membershipRank", user.getMembershipRank());
                map.put("points", user.getPoints());
                map.put("phone", user.getPhone());
                return ResponseEntity.ok(map);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}

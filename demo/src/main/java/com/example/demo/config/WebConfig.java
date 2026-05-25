package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Lấy đường dẫn tuyệt đối của thư mục upload
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
        String pathString = uploadPath.toUri().toString();
        
        // Cấu hình để truy cập ảnh qua URL /uploads/
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(pathString);
    }
}

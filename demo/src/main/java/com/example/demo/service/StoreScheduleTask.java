package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class StoreScheduleTask {

    @Autowired
    private SystemSettingService systemSettingService;

    @Autowired
    private RevenueService revenueService;

    // Run at 22:30 every day (Cron: second minute hour day month weekday)
    @Scheduled(cron = "0 30 22 * * ?")
    public void closeStoreAndCalculateRevenue() {
        // Đóng cửa hệ thống
        systemSettingService.setStoreStatus(false);
        
        // Tự động chốt doanh thu trong ngày (Logic tính toán sẵn sàng phục vụ)
        // Bản báo cáo snapshot có thể được lưu xuống DB ở đây nếu có bảng DailyRevenue
        // Hiện tại RevenueService tính trực tiếp (real-time).
        System.out.println("Hệ thống đã tự động đóng cửa và chốt doanh thu lúc 22:30.");
    }

    // Run at 06:30 every day
    @Scheduled(cron = "0 30 06 * * ?")
    public void openStore() {
        // Mở cửa hệ thống
        systemSettingService.setStoreStatus(true);
        System.out.println("Hệ thống đã tự động mở cửa lúc 06:30.");
    }
}

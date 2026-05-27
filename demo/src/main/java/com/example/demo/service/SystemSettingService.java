package com.example.demo.service;

import com.example.demo.model.SystemSetting;
import com.example.demo.repository.SystemSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class SystemSettingService {

    @Autowired
    private SystemSettingRepository settingRepository;

    public String getSetting(String key, String defaultValue) {
        return settingRepository.findByKey(key)
                .map(SystemSetting::getValue)
                .orElse(defaultValue);
    }

    public void updateSetting(String key, String value, String description, String group) {
        SystemSetting setting = settingRepository.findByKey(key)
                .orElse(SystemSetting.builder().key(key).build());
        setting.setValue(value);
        if (description != null) setting.setDescription(description);
        if (group != null) setting.setGroupName(group);
        settingRepository.save(setting);
    }
    
    // Quick getters for common settings
    public String getHotline() { return getSetting("hotline", "0123-456-789"); }
    public String getAddress() { return getSetting("address", "Hồ Chí Minh, Việt Nam"); }
    public boolean isMaintenanceMode() { return "true".equals(getSetting("maintenance_mode", "false")); }
    public double getPointRate() { return Double.parseDouble(getSetting("point_rate", "10000")); }
    
    // Store Status
    public boolean isStoreOpen() { return "OPEN".equals(getSetting("store_status", "OPEN")); }
    public void setStoreStatus(boolean isOpen) {
        updateSetting("store_status", isOpen ? "OPEN" : "CLOSED", "Trạng thái đóng/mở cửa của hệ thống", "SYSTEM");
    }
}

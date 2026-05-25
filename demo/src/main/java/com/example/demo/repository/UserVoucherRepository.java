package com.example.demo.repository;

import com.example.demo.model.User;
import com.example.demo.model.UserVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserVoucherRepository extends JpaRepository<UserVoucher, Long> {
    List<UserVoucher> findByUser(User user);
    List<UserVoucher> findByUserAndUsedFalse(User user);
}

package com.vatlieu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasRole('OWNER')")
public class SupplierService {
    private final JdbcTemplate jdbc;

    @Transactional
    public void delete(long id) {
        // Chỉ ngừng sử dụng, giữ nguyên liên kết phiếu nhập và công nợ cũ.
        if (jdbc.update("UPDATE supplier SET active=false WHERE id=?", id) == 0)
            throw new IllegalArgumentException("Nhà cung cấp không còn tồn tại");
    }
}

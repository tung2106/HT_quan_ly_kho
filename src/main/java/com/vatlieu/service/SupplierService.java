package com.vatlieu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasRole('OWNER')")
public class SupplierService {
    private final JdbcTemplate jdbc;
    private final com.vatlieu.repository.SupplierRepository repo;

    @Transactional(readOnly = true)
    public java.util.List<com.vatlieu.entity.Supplier> search(boolean active, String keyword) {
        String query = normalizeSearch(keyword.trim());
        var rows = active ? repo.findByActiveTrueOrderByNameAsc() : repo.findByActiveFalseOrderByNameAsc();
        return rows.stream().filter(s -> normalizeSearch(s.getName()).contains(query)
                || (s.getPhone() != null && s.getPhone().toLowerCase(java.util.Locale.ROOT).contains(query))).toList();
    }

    private String normalizeSearch(String value) {
        // Chỉ bỏ dấu khi so khớp; tên lưu và hiển thị vẫn giữ nguyên dấu tiếng Việt.
        return java.text.Normalizer.normalize(value.toLowerCase(java.util.Locale.ROOT), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd');
    }

    @Transactional
    public void edit(long id, String name, String phone) {
        if (name == null || name.isBlank() || name.trim().length() > 150)
            throw new IllegalArgumentException("Tên nhà cung cấp cần từ 1 đến 150 ký tự");
        String normalizedPhone = phone == null || phone.isBlank() ? null : phone.trim();
        if (normalizedPhone != null && normalizedPhone.length() > 30)
            throw new IllegalArgumentException("Số điện thoại không được quá 30 ký tự");
        if (jdbc.update("UPDATE supplier SET name=?,phone=? WHERE id=?", name.trim(), normalizedPhone, id) == 0)
            throw new IllegalArgumentException("Nhà cung cấp không còn tồn tại");
    }

    @Transactional
    public void restore(long id) {
        if (jdbc.update("UPDATE supplier SET active=true WHERE id=?", id) == 0)
            throw new IllegalArgumentException("Nhà cung cấp không còn tồn tại");
    }

    @Transactional
    public void delete(long id) {
        // Chỉ ngừng sử dụng, giữ nguyên liên kết phiếu nhập và công nợ cũ.
        if (jdbc.update("UPDATE supplier SET active=false WHERE id=?", id) == 0)
            throw new IllegalArgumentException("Nhà cung cấp không còn tồn tại");
    }
}

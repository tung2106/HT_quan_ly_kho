package com.vatlieu.service;

import com.vatlieu.entity.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasRole('OWNER')")
public class SupplierService {

  private final JdbcTemplate jdbc;
  private final com.vatlieu.repository.SupplierRepository repo;

  @Transactional(readOnly = true)
  public java.util.List<com.vatlieu.entity.Supplier> search(boolean active, String keyword) {
    String query = SearchText.normalize(keyword);
    var rows = active
      ? repo.findByActiveTrueOrderByNameAsc()
      : repo.findByActiveFalseOrderByNameAsc();
    return rows
      .stream()
      .filter(
        s ->
          SearchText.normalize(s.getName()).contains(query) ||
          (s.getPhone() != null && s.getPhone().toLowerCase(java.util.Locale.ROOT).contains(query))
      )
      .toList();
  }

  private String normalizeName(String name) {
    if (
      name == null || name.isBlank() || name.trim().length() > 150
    ) throw new IllegalArgumentException("Tên nhà cung cấp cần từ 1 đến 150 ký tự");
    return name.trim();
  }

  private String normalizePhone(String phone) {
    String normalizedPhone = phone == null || phone.isBlank() ? null : phone.trim();
    if (
      normalizedPhone != null && normalizedPhone.length() > 30
    ) throw new IllegalArgumentException("Số điện thoại không được quá 30 ký tự");
    return normalizedPhone;
  }

  @Transactional
  public void create(String name, String phone) {
    Supplier supplier = new Supplier();
    supplier.setName(normalizeName(name));
    supplier.setPhone(normalizePhone(phone));
    repo.save(supplier);
  }

  @Transactional
  public void edit(long id, String name, String phone) {
    if (
      jdbc.update(
        "UPDATE supplier SET name=?,phone=? WHERE id=?",
        normalizeName(name),
        normalizePhone(phone),
        id
      ) == 0
    ) throw new IllegalArgumentException("Nhà cung cấp không còn tồn tại");
  }

  @Transactional
  public void restore(long id) {
    if (
      jdbc.update("UPDATE supplier SET active=true WHERE id=?", id) == 0
    ) throw new IllegalArgumentException("Nhà cung cấp không còn tồn tại");
  }

  @Transactional
  public void delete(long id) {
    // Chỉ ngừng sử dụng, giữ nguyên liên kết phiếu nhập và công nợ cũ.
    if (
      jdbc.update("UPDATE supplier SET active=false WHERE id=?", id) == 0
    ) throw new IllegalArgumentException("Nhà cung cấp không còn tồn tại");
  }
}

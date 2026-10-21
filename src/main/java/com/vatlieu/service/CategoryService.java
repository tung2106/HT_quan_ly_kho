package com.vatlieu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasRole('OWNER')")
public class CategoryService {
    private final JdbcTemplate jdbc;
    @Transactional public void save(Long id,String name,boolean perishable) {
        if(name==null || name.isBlank() || name.trim().length()>100) throw new IllegalArgumentException("Tên danh mục cần từ 1 đến 100 ký tự");
        if(id==null) jdbc.update("INSERT INTO category(name,perishable) VALUES (?,?)",name.trim(),perishable);
        else if(jdbc.update("UPDATE category SET name=?,perishable=? WHERE id=?",name.trim(),perishable,id)==0)
            throw new IllegalArgumentException("Danh mục không còn tồn tại");
    }
    @Transactional public void delete(long id) {
        if(jdbc.queryForList("SELECT id FROM category WHERE id=? FOR UPDATE",id).isEmpty()) throw new IllegalArgumentException("Danh mục không còn tồn tại");
        if(jdbc.queryForObject("SELECT COUNT(*) FROM product WHERE category_id=?",Integer.class,id)>0)
            throw new IllegalArgumentException("Không thể xóa danh mục đang có sản phẩm. Hãy chuyển sản phẩm sang danh mục khác trước.");
        jdbc.update("DELETE FROM category WHERE id=?",id);
    }
}

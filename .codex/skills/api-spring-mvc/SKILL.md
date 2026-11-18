---
name: api-spring-mvc
description:
  Dùng khi thêm hoặc sửa đường dẫn HTTP của web kho, tham số form, bộ lọc GET, phản hồi POST, CSRF
  hoặc quyền truy cập theo từng phiếu.
---

# API · Yêu cầu HTTP và phân quyền

1. **Xác định hợp đồng HTTP.** Web dùng Spring MVC để nhận form và trả trang Thymeleaf. Xem tên
   trường trong template, kiểu dữ liệu ở form và mapping của controller. Ví dụ,
   `GET /outbound?trash=true` mở Thùng rác, còn `POST /outbound/{id}/restore` yêu cầu khôi phục một
   phiếu. [OperationsController](../../../src/main/java/com/vatlieu/web/OperationsController.java)
2. **Giữ thao tác xem an toàn.** Dùng GET để xem hoặc lọc. Dùng POST với CSRF cho thao tác ghi. Sau
   khi lưu thành công, chuyển về trang phù hợp để tải lại trang không gửi lại form. Khi nhập sai,
   giữ lại giá trị hữu ích và báo cụ thể trường hoặc điều kiện chưa đạt.
3. **Kiểm tra quyền ở máy chủ.** Đối chiếu
   [SecurityConfig](../../../src/main/java/com/vatlieu/config/SecurityConfig.java) và service xử lý
   phiếu. Quản lý được sử dụng Thùng rác. Nhân viên chỉ xem phiếu bán đang sử dụng do mình tạo. Kiểm
   tra cả truy cập thẳng theo ID và dữ liệu trả về, kể cả giá vốn.
4. **Kiểm tra với MockMvc.** Thử khách chưa đăng nhập, nhân viên và quản lý. Với POST, thử thiếu
   CSRF, sai dữ liệu và gửi thành công. Kiểm tra mã HTTP, trang chuyển đến và việc sổ kho có thay
   đổi đúng một lần.
   [AccessPostgresTest](../../../src/test/java/com/vatlieu/AccessPostgresTest.java)

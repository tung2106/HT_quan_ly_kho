---
name: database-postgres-flyway
description:
  Dùng khi thêm cột, ràng buộc, migration Flyway, truy vấn PostgreSQL hoặc khóa dữ liệu để tồn kho,
  tiền và công nợ nhất quán.
---

# Database · PostgreSQL và Flyway

1. **Xác định dữ liệu gốc.** Tồn của một sản phẩm lấy từ tổng `stock_transaction.qty_change`. Vốn
   hàng còn lấy từ tổng `batch.qty_left × batch.unit_cost`. Dùng
   [StockService](../../../src/main/java/com/vatlieu/service/StockService.java) để đối chiếu truy
   vấn. Một đơn vị hàng xuất phải có thể lần về lô tương ứng.
2. **Giữ dữ liệu cũ đọc được.** Xem phiên bản lớn nhất trong
   [db/migration](../../../src/main/resources/db/migration), rồi thêm file
   `V<số tiếp theo>__<mô tả>.sql`. Giữ nguyên các migration đã chạy. Ví dụ, khi thêm trạng thái
   Thùng rác, giá trị mặc định phải để phiếu cũ tiếp tục xuất hiện trong lịch sử. Dự án dùng Flyway
   để đổi schema và Hibernate để kiểm tra ánh xạ.
3. **Xử lý hai thao tác cùng lúc.** Với thu tiền và xóa cùng một phiếu, khóa phiếu trước khi đọc số
   tiền hoặc đổi trạng thái. Với nhiều sản phẩm, khóa theo thứ tự ID thống nhất với luồng xuất. Ràng
   buộc ở CSDL và mã lần gửi bảo vệ các luồng tạo phiếu, thanh toán khi người dùng bấm lại.
4. **Thử migration riêng.** Trỏ `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
   `SPRING_DATASOURCE_PASSWORD` tới CSDL kiểm thử, bật `RUN_POSTGRES_TESTS=true` rồi chạy
   `mvn test`. Kiểm tra cả khởi tạo mới lẫn nâng cấp từ dữ liệu cũ có liên quan. Đối chiếu sổ giao
   dịch, tồn từng lô và giá vốn sau thao tác.

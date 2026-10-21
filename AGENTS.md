# AGENTS.md — Hướng dẫn cho AI làm việc trong dự án này

## Mục tiêu dự án
Hệ thống quản lý kho và lợi nhuận cho cửa hàng vật liệu xây dựng và nông sản
(gạch, cát, xi măng, sắn, ngô, củ quả). Bán lẻ và bán sỉ, nhập hàng không cố định.
Người dùng: 5-6 người trong gia đình, chủ yếu dùng trên điện thoại.

## Công nghệ
Java 17, Spring Boot 3.3, Spring Data JPA, PostgreSQL, Flyway, Thymeleaf + Bootstrap 5, Lombok, JUnit 5.

## Lệnh thường dùng
- Chạy CSDL: `docker compose up -d`
- Chạy app: `mvn spring-boot:run`
- Chạy test: `mvn test`
- Trước khi báo xong một việc: `mvn test` phải qua và app phải khởi động được.

## Cấu trúc
- `entity/` ánh xạ bảng · `repository/` truy vấn · `service/` nghiệp vụ
- `web/` controller · `form/` dữ liệu từ form · `resources/templates/` giao diện
- `resources/db/migration/` các file Flyway

## Quy tắc bắt buộc
1. Tiền và số lượng dùng `BigDecimal`, không dùng `double`/`float`.
2. Kho luôn lưu theo ĐƠN VỊ GỐC của sản phẩm; tồn kho tính từ `stock_transaction`
   (qty_change dương = nhập, âm = xuất), KHÔNG sửa tồn trực tiếp.
3. Giá vốn: bình quân gia quyền ở `product.avg_cost` + giá vốn theo lô ở `batch`.
   Khi xuất kho (bán, hao hụt) trừ theo FIFO từ `batch.qty_left` và ghi lại giá vốn tại thời điểm xuất.
4. Mọi thay đổi CSDL phải thêm file Flyway mới (`V2__...sql`, `V3__...sql`).
   KHÔNG sửa file migration đã có.
5. Logic nghiệp vụ nằm trong `service/`; controller chỉ nhận và trả dữ liệu.
6. Chỉ sửa đúng phạm vi được yêu cầu. KHÔNG viết lại cả file, đổi cấu trúc, đổi tên hàng loạt
   hay nâng phiên bản thư viện khi chưa được hỏi.
7. Mỗi chức năng tính toán tiền/giá vốn/lợi nhuận phải có unit test, gồm các trường hợp biên
   (số lẻ, bán đúng bằng tồn, bán vượt tồn, nhiều lô giá khác nhau, quy đổi đơn vị).
8. Giao diện ưu tiên điện thoại: nút to, ít thao tác, hiển thị tiền dạng 1.234.567.
9. Không hardcode mật khẩu hay khóa bí mật; dùng biến môi trường.
10. Khi chưa chắc về nghiệp vụ (cách tính, làm tròn, quy ước), HỎI lại thay vì tự đoán.

## Cách trả lời
- Giải thích ngắn gọn bằng tiếng Việt; comment trong code bằng tiếng Việt.
- Cuối mỗi việc: liệt kê file đã tạo/sửa, cách chạy thử, các trường hợp biên đã xử lý,
  và những gì CHƯA làm.

## Phạm vi hiện tại (cập nhật khi xong từng bước)
- Đã có (Bước 2): danh mục, sản phẩm, quy đổi đơn vị, nhà cung cấp, nhập hàng, tồn kho;
  xuất bán nội bộ, hao hụt FIFO, công nợ theo phiếu và lịch sử thu/trả,
  tổng hợp vốn tồn, doanh thu, giá vốn, hao hụt và chênh lệch thu/trả.
- Quy ước đã chốt: cộng tất cả dòng rồi làm tròn tổng đến 1 đồng (HALF_UP),
  số lượng gốc tối đa 3 số lẻ; sau xuất tính lại giá vốn bình quân từ các lô còn lại.
- Chưa có: giá sỉ theo bậc, kiểm kê điều chỉnh, sửa/hủy/hoàn phiếu, báo cáo theo kỳ,
  quỹ đầu kỳ/vốn góp/rút/chi phí khác, gợi ý giải pháp, triển khai.
- Đã bổ sung: đăng nhập, quản lý cấp/khóa/đặt lại mật khẩu; OWNER toàn quyền,
  STAFF chỉ xem tồn/giá bán và tạo/xem phiếu bán của mình; CSRF và kiểm tra quyền máy chủ.
  Quản lý thêm/sửa/xóa danh mục (chặn xóa khi còn sản phẩm); nút quay lại; phiếu nhập một dòng.
- Nhà cung cấp: quản lý xóa khỏi danh sách bằng cách ngừng sử dụng; không cho chọn khi nhập mới,
  vẫn giữ tên trên phiếu cũ, công nợ và cho phép trả nợ tiếp.

---
name: frontend-thymeleaf
description:
  Dùng khi sửa giao diện web kho trên điện thoại, template Thymeleaf, biểu mẫu nhập tiền, điều
  hướng, hộp xác nhận hoặc trạng thái nút.
---

# Frontend · Giao diện trên điện thoại

1. **Tìm màn hình.** Đọc template của chức năng cùng
   [quy ước tương tác](../../../docs/giao-dien/UX-CONTRACT.md) và
   [thiết kế giao diện](../../../docs/giao-dien/DESIGN.md). Ví dụ, lịch sử xuất nằm ở
   `outbound/list.html`, còn chi tiết một phiếu nằm ở `outbound/detail.html`.
2. **Dùng thành phần sẵn có.** Dùng
   [fragments.html](../../../src/main/resources/templates/fragments.html) cho thanh điều hướng và
   thông báo. Hộp xác nhận dùng `data-confirm` của
   [ui.js](../../../src/main/resources/static/js/ui.js). Giữ tiêu điểm ở nút Hủy khi hộp mở và trả
   về nút vừa bấm khi đóng.
3. **Giữ đúng dữ liệu form.** Ô tiền dùng
   [money-input.js](../../../src/main/resources/static/js/money-input.js). Khi nhập `1250000`, người
   dùng thấy `1.250.000 VNĐ` và máy chủ nhận giá trị số. Số lượng `6` vẫn hiện `6`. Giữ tên trường,
   nhãn liên kết và giá trị người dùng đã nhập khi máy chủ trả lỗi.
4. **Thử bằng thao tác thật.** Kiểm tra ở chiều rộng 360 và 390 px, với tên hàng dài và số tiền lớn.
   Với nút xóa, thử Hủy, Escape và xác nhận. Khi đổi JavaScript dùng chung, chạy `npm test` và thử
   thêm một màn hình khác có cùng thành phần.

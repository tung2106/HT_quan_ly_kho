---
name: backend-nghiep-vu-kho
description:
  Dùng khi sửa nghiệp vụ Java của cửa hàng như nhập hoặc xuất theo lô, FIFO, tính vốn, làm tròn
  tiền, ghi công nợ, tất toán hoặc khôi phục phiếu.
---

# Backend · Nghiệp vụ kho và tiền

1. **Theo đường đi của một phiếu.** Bắt đầu ở
   [PurchaseService](../../../src/main/java/com/vatlieu/service/PurchaseService.java) cho nhập hàng
   hoặc [OperationsService](../../../src/main/java/com/vatlieu/service/OperationsService.java) cho
   xuất và thu trả. Đặt phép tính và điều kiện ghi sổ trong service để các màn hình dùng chung.
2. **Tính theo đơn vị gốc.** Dùng `BigDecimal` và các hàm của
   [InventoryMath](../../../src/main/java/com/vatlieu/service/InventoryMath.java). Số lượng sau quy
   đổi có tối đa ba chữ số thập phân. Cộng các dòng rồi làm tròn tổng đến một đồng bằng `HALF_UP`.
   Nông sản lấy kg cân thực tế.
3. **Ghi đầy đủ trong một giao dịch.** Cập nhật phiếu, biến động kho, lô và khoản thu hoặc trả liên
   quan trong cùng transaction. Khi bán, dùng lô đã chọn hoặc FIFO nếu để trống. Khi khôi phục phiếu
   bán, kiểm tra đủ số lượng ở từng lô gốc trước khi trừ hàng. Đối chiếu
   [SaleTrashService](../../../src/main/java/com/vatlieu/service/SaleTrashService.java).
4. **Chứng minh kết quả.** Với thay đổi phép tính, viết unit test cho trường hợp biên có liên quan.
   Ví dụ, bán từ hai lô khác giá rồi hủy và khôi phục phải cho lại đúng số lượng, giá vốn và công nợ
   ban đầu. Dùng
   [OperationsPostgresTest](../../../src/test/java/com/vatlieu/OperationsPostgresTest.java) để kiểm
   tra các bút toán và việc gửi lặp trên PostgreSQL riêng.

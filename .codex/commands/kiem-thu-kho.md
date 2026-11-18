# Kiểm thử dự án kho

**Cách gọi trong Codex** `$kiem-thu-kho`. Có thể ghi thêm chức năng cần kiểm tra, ví dụ
`$kiem-thu-kho khôi phục phiếu bán`.

1. Xác định phạm vi từ yêu cầu và các tệp đã thay đổi. Xem cách chạy phù hợp với máy trong
   [hướng dẫn dự án](../../docs/HUONG-DAN-DU-AN.md).
2. Chạy `mvn test` với PostgreSQL kiểm thử riêng và `RUN_POSTGRES_TESTS=true` khi kiểm tra tích hợp.
   Dùng `npm test` cho JavaScript và `npm run format:check` để rà định dạng mã nguồn.
3. Khởi động ứng dụng với dữ liệu thử. Thử luồng chính và một trường hợp bị từ chối. Ví dụ, với khôi
   phục phiếu bán, thử một phiếu đủ hàng và một phiếu thiếu hàng ở lô gốc.
4. Báo kết quả từng phép kiểm tra, số test đạt, lỗi, số bị bỏ qua và bước chưa chạy. Nếu phát hiện
   lỗi, ghi cách tái hiện trên dữ liệu thử.

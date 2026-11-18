# Rà soát thay đổi

**Cách gọi trong Codex** `$ra-soat-kho`. Có thể chỉ định tệp hoặc chức năng, ví dụ
`$ra-soat-kho phần thu nợ khách hàng`.

1. Xem `git status --short --branch`, diff đã stage và chưa stage. Đọc thêm tệp mới thuộc phạm vi
   cần rà. Nếu người dùng chỉ định một chức năng, lần theo đường đi của chức năng đó.
2. Chọn skill tương ứng trong [mục lục Codex](../README.md). Ví dụ, một POST thu nợ cần đối chiếu
   quyền, số nợ còn lại và cách xử lý hai lần bấm trên cùng phiếu.
3. Báo lỗi theo mức ảnh hưởng, kèm tệp, vị trí, điều kiện xảy ra và hệ quả. Phân biệt lỗi đã tái
   hiện với điểm cần xác minh. Nếu chưa phát hiện lỗi, ghi phạm vi đã xem và trường hợp còn thiếu
   kiểm thử.

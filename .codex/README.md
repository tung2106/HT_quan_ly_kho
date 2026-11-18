# Làm việc với Codex trong dự án kho

Mở Codex tại thư mục chứa `pom.xml` và `AGENTS.md`. Bạn có thể mô tả việc cần làm bằng tiếng Việt.
Mỗi skill dưới đây có một tệp `SKILL.md` gồm mô tả khi nào dùng và các bước thực hiện.

## Skill theo công việc

| Khi bạn yêu cầu                                              | Codex có thể dùng                                                           |
| ------------------------------------------------------------ | --------------------------------------------------------------------------- |
| “Sửa ô nhập tiền để dễ bấm trên điện thoại.”                 | [Frontend · Thymeleaf và biểu mẫu](skills/frontend-thymeleaf/SKILL.md)      |
| “Kiểm tra cách tính giá vốn khi bán từ hai lô.”              | [Backend · Nghiệp vụ kho và tiền](skills/backend-nghiep-vu-kho/SKILL.md)    |
| “Thêm cột ghi ngày khôi phục phiếu, giữ được dữ liệu cũ.”    | [Database · PostgreSQL và Flyway](skills/database-postgres-flyway/SKILL.md) |
| “Chặn nhân viên mở phiếu bán của người khác bằng đường dẫn.” | [API · HTTP và phân quyền](skills/api-spring-mvc/SKILL.md)                  |

Một việc có thể dùng nhiều skill. Chẳng hạn, thêm bộ lọc ngày vào lịch sử bán liên quan đến form ở
giao diện và tham số GET ở controller.

## Thao tác gọi tay

| Gõ trong ô chat Codex               | Kết quả                                                                         |
| ----------------------------------- | ------------------------------------------------------------------------------- |
| `$kiem-thu-kho khôi phục phiếu bán` | [Chạy kiểm thử](commands/kiem-thu-kho.md) và báo rõ phần đã kiểm tra.           |
| `$ra-soat-kho phần thu nợ`          | [Đọc và rà soát thay đổi](commands/ra-soat-kho.md), nêu lỗi theo mức ảnh hưởng. |

Tệp trong `commands/` chứa quy trình gọi tay. Mỗi quy trình có một skill cùng tên dẫn tới tệp đó.
Chính sách `allow_implicit_invocation: false` trong `agents/openai.yaml` dành hai thao tác này cho
lúc bạn gọi bằng `$`.

## Thử cách chọn skill

Trong một cuộc trò chuyện mới của dự án, nhập “Số lượng 6 đang hiện thành 6.000 trên điện thoại. Hãy
xem phần giao diện.” Skill phù hợp là `frontend-thymeleaf`. Để chọn đích danh, gõ
`$frontend-thymeleaf` trước yêu cầu.
[Cách Codex chọn và gọi skill](https://developers.openai.com/codex/skills/)

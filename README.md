# Kho gia đình

Web quản lý nhập hàng, xuất bán, hao hụt, vốn và công nợ cho cửa hàng vật liệu xây dựng và nông sản.
Mỗi lần nhập tạo một lô. Khi bán có thể chọn lô hoặc dùng FIFO.

## Mở ứng dụng

Trên máy Windows đã cài bộ công cụ của cửa hàng, nhấp đúp `Mo-app.cmd` ở thư mục gốc rồi mở
[web trên máy](http://127.0.0.1:18080/).

Trên máy mới, xem
[cách cấu hình PostgreSQL, chạy Maven và kiểm thử](docs/HUONG-DAN-DU-AN.md#6-chạy-và-kiểm-thử). Để
cấp tài khoản quản lý đầu tiên trên cơ sở dữ liệu trống, đặt `APP_ADMIN_USERNAME` và
`APP_ADMIN_PASSWORD` trước khi chạy. Quản lý cấp tài khoản cho các thành viên trong **Cài đặt → Quản
lý tài khoản**.

## Tìm tài liệu

| Công việc                              | Tài liệu                                                                                   |
| -------------------------------------- | ------------------------------------------------------------------------------------------ |
| Bắt đầu sửa dự án                      | [Hướng dẫn dự án](docs/HUONG-DAN-DU-AN.md) và [quy tắc cho Codex](AGENTS.md)               |
| Chọn skill hoặc gọi quy trình kiểm thử | [Skill và lệnh Codex](.codex/README.md)                                                    |
| Làm giao diện                          | [Thiết kế](docs/giao-dien/DESIGN.md) và [quy ước tương tác](docs/giao-dien/UX-CONTRACT.md) |
| Xem kết quả các đợt kiểm thử           | [Báo cáo chất lượng](docs/kiem-thu/KIEM-TRA-CHAT-LUONG.md)                                 |
| Đưa web lên mạng                       | [Triển khai Neon và Render](docs/trien-khai/TRIEN-KHAI-NEON-RENDER.md)                     |
| Tra cứu phiên bản đầu                  | [Ghi chép bước 2](docs/lich-su/BUOC-2.md)                                                  |

## Tìm mã nguồn

| Thư mục                                                 | Nội dung                                                                                                                                 |
| ------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| [src/main/java/com/vatlieu](src/main/java/com/vatlieu/) | Java theo tầng, gồm controller, form, service, repository, entity và cấu hình.                                                           |
| [templates](src/main/resources/templates/)              | Các nhóm trang `account/`, `catalog/`, `purchases/`, `outbound/` và `stock/`. Trang chủ, vốn và thành phần dùng chung ở gốc thư mục này. |
| [static](src/main/resources/static/)                    | CSS, JavaScript và hình ảnh.                                                                                                             |
| [db/migration](src/main/resources/db/migration/)        | Các phiên bản lược đồ PostgreSQL do Flyway quản lý.                                                                                      |
| [src/test](src/test/)                                   | Kiểm thử Java và JavaScript.                                                                                                             |

Chạy lệnh Maven, npm và Docker Compose từ thư mục gốc chứa `pom.xml`. Hai tệp `Mo-app.cmd`,
`Mo-app.ps1` cũng nằm ở đây để mở ứng dụng trên Windows.

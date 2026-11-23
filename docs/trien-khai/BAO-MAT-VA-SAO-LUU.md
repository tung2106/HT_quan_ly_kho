# Bảo mật và sao lưu Kho gia đình

## Đối chiếu 20 tiêu chí

| Mục | Cơ chế và phạm vi kiểm tra |
| --- | --- |
| 01. Băm mật khẩu | Tạo tài khoản, đổi và đặt lại mật khẩu đều dùng BCrypt. |
| 02. Hạn chế đăng nhập | PostgreSQL đếm theo tên chuẩn hóa và cả web trước bước so mật khẩu. Hết thời gian chờ sẽ tự mở. |
| 03. Hết phiên | 30 phút không hoạt động. Đổi mật khẩu, thu hồi quyền, khóa hoặc xóa tài khoản làm mất hiệu lực phiên. |
| 04–06. Log, secret, lỗi | Không thấy log debug viết tay hoặc khóa dịch vụ trong frontend của bản đã rà. Biến bí mật ở máy chủ. Profile prod ẩn thông báo lỗi nội bộ, lỗi binding và stack trace. Người dùng vẫn thấy lỗi nhập liệu do ứng dụng định nghĩa. |
| 07–08. Loại và dung lượng tệp | Không có chức năng nhận tệp người dùng. Bộ xử lý multipart tự động đã tắt. Ảnh mèo được đóng gói cùng mã nguồn. |
| 09. Kiểm tra dữ liệu | Service kiểm tra mật khẩu, vai trò, số lượng, đơn vị, tồn lô và tiền thanh toán trước khi ghi. |
| 10–11. Quyền đối tượng và quản trị | Cả hai vai trò cùng quản lý một kho. Kiểm tra quy đổi thuộc đúng sản phẩm. Chỉ OWNER quản trị tài khoản; các thao tác xóa và đặt lại mật khẩu qua trang quản trị chỉ áp dụng cho STAFF. Có kiểm thử bằng dữ liệu riêng. |
| 12. SQL | Giá trị đầu vào được truyền bằng tham số JDBC/JPA trong các truy vấn đã rà. Chọn bảng thu hoặc trả dựa trên hai tên cố định do mã nguồn quy định. |
| 13–15. Trình duyệt | Render chuyển HTTP sang HTTPS. Spring Security cấu hình CSP, Referrer-Policy và các header mặc định, gồm HSTS cho HTTPS. Cookie phiên prod dùng Secure, HttpOnly, SameSite=Lax. |
| 16. CORS | Form và API dùng cùng origin. Không thấy cho phép đọc dữ liệu từ origin ngoài. CSRF áp dụng cho thao tác ghi. |
| 17. Mạng của DB | Docker chỉ mở cổng ở loopback. Ngày 23/11/2026, dự án Neon cho phép kết nối công khai qua TLS, danh sách IP được phép trống. |
| 18. Quyền DB | Có script cấp riêng quyền ứng dụng và quyền đọc sao lưu, đã thử với database riêng. Phải đổi thông tin kết nối trên Render để áp dụng với dữ liệu thật. |
| 19. Cloudflare | Gia đình chọn dùng tên miền con của Render. Chưa cấu hình zone, proxy hoặc bộ quy tắc Cloudflare riêng. |
| 20. Sao lưu và báo lỗi | Khi rà soát, Neon giữ lịch sử khôi phục 6 giờ. Bản sao Drive cần quyền Google, thông tin đọc Neon và lần chạy thành công. Render có health check; GitHub kiểm tra thêm trang đăng nhập theo lịch. Theo dõi lỗi ứng dụng trong Render Logs. |

## Tài khoản PostgreSQL trên Render

Web và Flyway có thể kết nối PostgreSQL bằng hai tài khoản riêng. Thực hiện theo thứ tự này để giữ web hoạt động.

1. Trong đúng database của kho, dùng tài khoản đã tạo schema để chạy [quyen-postgres.sql](quyen-postgres.sql). Role `kho_app` chỉ có quyền nghiệp vụ. Role `kho_backup` chỉ đọc dữ liệu và sequence. Khi bảng nghiệp vụ mới ra đời, cấp thêm quyền cần thiết trong migration.
2. Trong phiên `psql` qua TLS, dùng `\password kho_app` và `\password kho_backup` để đặt hai mật khẩu khác nhau. Dùng tên role mới dành riêng cho ứng dụng. Kiểm tra chúng không sở hữu database/schema và không thừa hưởng quyền quản trị qua role khác.
3. Trong Render → Environment, cấu hình `SPRING_FLYWAY_USER` và `SPRING_FLYWAY_PASSWORD` bằng tài khoản chủ schema đang chạy migration. Trong cùng lần cập nhật, đổi `NEON_DATABASE_USERNAME` và `NEON_DATABASE_PASSWORD` sang `kho_app`. Giữ kết nối TLS xác minh tên máy chủ. Sau khi triển khai, kiểm tra đăng nhập, đọc kho và log Flyway.
4. PostgreSQL Docker trên máy chỉ công bố cổng trên loopback. Với Neon, TLS bảo vệ kết nối nhưng endpoint vẫn có thể truy cập từ Internet. Muốn giới hạn mạng, xem tùy chọn IP Allow hoặc mạng riêng của gói đang dùng. Trước khi thay đổi phải tính cả kết nối từ Render và máy chạy sao lưu. [Mạng riêng và IP Allow của Neon](https://neon.com/docs/introduction/ip-allow)

## Sao lưu vào Google Drive riêng

[Workflow sao lưu](../../.github/workflows/backup.yml) chạy trên GitHub Actions. Cần quyền của chủ Drive một lần khi thiết lập. Mỗi lần chạy tạo một tệp mới trong **KhoGiaDinh-Backup**. Dữ liệu được mã hóa trước khi gửi và bản tải lên được đối chiếu SHA-256. Dùng thư mục Drive ở chế độ **Bị hạn chế**. Theo dõi dung lượng Drive vì workflow giữ lại các bản cũ.

1. Trên máy cá nhân, dùng `age-keygen -o kho-backup.key` trong thư mục riêng nằm ngoài repo. Giữ khóa riêng ở nơi an toàn và có một bản dự phòng. Chép khóa **công khai** bắt đầu bằng `age1` vào biến Actions **BACKUP_AGE_RECIPIENT**. Mất khóa riêng sẽ không thể giải mã bản sao. [Công cụ age](https://github.com/FiloSottile/age)
2. Trên máy cá nhân, chạy `rclone config`, tạo remote kiểu **Google Drive**, đặt tên `khodrive`, để trống client ID/secret để dùng client mặc định, chọn phạm vi **drive.file** (chỉ các tệp do rclone tạo), đăng nhập đúng Google Drive riêng và cấp quyền. Trong file cấu hình rclone trên máy (xem đường dẫn bằng `rclone config file`), nhập riêng giá trị JSON của trường `token` của remote này vào secret **BACKUP_DRIVE_TOKEN**. Token là thông tin bí mật. [Thiết lập rclone với Google Drive](https://rclone.org/drive/)
3. Tại **Settings → Secrets and variables → Actions** của repo, thêm các **Secrets** `BACKUP_PGHOST` (host Neon trực tiếp), `BACKUP_PGDATABASE`, `BACKUP_PGUSER` (role `kho_backup`), `BACKUP_PGPASSWORD` và token Drive ở trên. Chỉ khóa công khai của age và công tắc bật lịch nằm trong **Variables**.
4. Trong **Actions → Sao lưu kho và kiểm tra web → Run workflow**, chạy thử trên `main`. Kiểm tra job `backup` thành công và tệp `.dump.age` đã có trong Drive. Tải về máy, giải mã bằng `age -d -i kho-backup.key -o kho.dump ten-tep.dump.age`. Dùng `pg_restore --exit-on-error --no-owner --no-privileges -d kho_thu kho.dump` với một database thử **trống**, đối chiếu phiếu và số dư. Giữ tệp đã giải mã trong vùng lưu trữ riêng.
5. Khi chạy thử thành công, đặt **Variable** `DRIVE_BACKUP_ENABLED=true`. Lịch chạy khoảng 03:23 giờ Việt Nam hằng ngày trên nhánh mặc định. Có thể chạy tay kể cả trước khi bật lịch. Nếu thu hồi quyền Drive, tạo lại token rồi chạy thử lại.

## Theo dõi lỗi

Render kiểm tra `/login` để xác nhận web đã khởi động. Trong Render → Notifications, xác nhận địa chỉ nhận **Only failure notifications**. GitHub Actions kiểm tra trang đăng nhập mỗi ngày và báo trạng thái của job sao lưu riêng. Trong phần Notifications của tài khoản GitHub, bật email cho các lần chạy Actions thất bại và kiểm tra lần chạy gần nhất trong tab Actions. Các kiểm tra này không bao phủ mọi lỗi giao dịch. [Thông báo Render](https://render.com/docs/notifications)

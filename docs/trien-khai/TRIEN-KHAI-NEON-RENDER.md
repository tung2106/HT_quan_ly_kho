# Đưa ứng dụng lên mạng với Neon Free và Render Free

Cấu hình chuẩn bị ngày 16/11/2026. Neon lưu PostgreSQL; Render chạy ứng dụng Java.
Chỉ tạo Neon chưa đủ để mở web. Bản này cần Internet, chưa phải ứng dụng Android offline.

## 1. Chọn dữ liệu trước khi triển khai

- **Thử với dữ liệu trống:** tạo một dự án Neon mới. Lần chạy đầu, Flyway tự tạo cấu trúc
  CSDL bằng các migration có sẵn. Sau đó tạo tài khoản quản lý đầu tiên.
- **Chuyển dữ liệu đang dùng:** cần sao lưu và chuyển CSDL trước khi mở bản online cho gia đình.
  Không chạy đồng thời hai bản độc lập để ghi giao dịch rồi mong chúng tự đồng bộ.
  Chưa có chức năng đồng bộ hoặc tự chuyển dữ liệu trong cấu hình này.

Không đưa mật khẩu, chuỗi kết nối có mật khẩu hoặc bản sao CSDL lên GitHub.
GitHub chỉ giữ mã nguồn. Không cần tải `.tools`, `.m2`, `target` hay `node_modules` lên.

## 2. Tạo CSDL Neon Free

1. Đăng nhập [Neon Console](https://console.neon.tech/).
2. Tạo project, ví dụ `kho-gia-dinh`, chọn **Free**. Chọn PostgreSQL 16 để cùng phiên bản
   CSDL đang dùng trên máy; ưu tiên Singapore nếu vùng đó có sẵn.
3. Mở **Connect**, chọn đúng branch và database. Tắt **Connection pooling** để lấy host trực tiếp
   (tên host không chứa `-pooler`). App chỉ mở tối đa 5 kết nối.
4. Ghi nhận host, tên database và role. Nhập mật khẩu trực tiếp vào Environment trên Render,
   không gửi vào chat hay lưu trong tệp dự án.

Ví dụ, nếu Neon hiện `postgresql://<role>:<password>@ep-example.ap-southeast-1.aws.neon.tech/neondb?...`,
điền `ep-example.ap-southeast-1.aws.neon.tech` vào ô host trên Render và `neondb` vào ô database.
Ứng dụng tự tạo URL và dùng `verify-full` để kiểm tra cả chứng chỉ TLS lẫn tên máy chủ qua kho chứng chỉ tin cậy của Java. [Tài liệu pgJDBC](https://jdbc.postgresql.org/documentation/ssl/)

## 3. Đưa phiên bản đã kiểm tra lên GitHub

Repository: [tung2106/HT_quan_ly_kho](https://github.com/tung2106/HT_quan_ly_kho).
Blueprint sử dụng nhánh `main`. Cần có đủ mã nguồn hiện tại cùng các tệp triển khai mới;
nếu chỉ đẩy Dockerfile mà bỏ sót các file Java/template/migration mới thì bản online sẽ thiếu chức năng.

Kiểm tra trước khi commit, chạy tại thư mục dự án:

```powershell
git status --short
git diff --stat
git diff --cached --stat
```

Chỉ commit và push sau khi rà các file được đưa lên. Chạy `mvn test`, test PostgreSQL trên
CSDL riêng và `npm test` trước khi triển khai. Không dùng CSDL cửa hàng để chạy test.

## 4. Tạo web trên Render

1. Đăng nhập [Render Dashboard](https://dashboard.render.com/).
2. Chọn **New → Blueprint**, kết nối GitHub và chọn repository trên, nhánh `main`.
3. Render đọc `render.yaml`. Kiểm tra chỉ có **một Web Service**, runtime Docker,
   vùng Singapore và gói **Free ($0)**. Không tạo thêm PostgreSQL trên Render.
4. Nhập các biến được yêu cầu:

| Biến                     | Giá trị                                                                                                  |
| ------------------------ | -------------------------------------------------------------------------------------------------------- |
| `NEON_DATABASE_HOST`     | Tên host trực tiếp lấy ở bước 2                                                                          |
| `NEON_DATABASE_NAME`     | Tên database lấy ở bước 2, thường là `neondb`                                                            |
| `NEON_DATABASE_USERNAME` | Role trong màn hình Connect của Neon                                                                     |
| `NEON_DATABASE_PASSWORD` | Mật khẩu của role Neon                                                                                   |
| `APP_ADMIN_USERNAME`     | Tên quản lý do bạn chọn, 3–50 ký tự thường, số, dấu chấm, gạch dưới hoặc gạch ngang; bắt đầu bằng chữ/số |
| `APP_ADMIN_PASSWORD`     | Mật khẩu khởi tạo do bạn đặt, ít nhất 12 ký tự, tối đa 72 byte UTF-8                                     |

`SPRING_PROFILES_ACTIVE=prod` đã được khai báo. Render cấp `PORT` tự động.
Nếu đã cấu hình bản cũ bằng `NEON_DATABASE_URL`, thêm hai biến host và database trong Environment
trước khi triển khai phiên bản này. Sau khi kiểm tra web chạy được, có thể xóa biến URL cũ.
Không thêm `SPRING_DATASOURCE_*` từ máy local: chúng có thể ghi đè cấu hình Neon.
Dockerfile đặt múi giờ Việt Nam; kết nối PostgreSQL cũng đặt cùng múi giờ để giữ ngày phiếu.

5. Bấm triển khai sau khi kiểm tra gói miễn phí. Đợi build và khởi động hoàn tất.
   Lần đầu sẽ chạy các migration và tạo quản lý nếu CSDL chưa có tài khoản.
6. Mở địa chỉ **HTTPS** do Render cấp, dạng `https://<ten-web>.onrender.com`.
   Điện thoại dùng Wi-Fi hoặc 4G đều mở được, máy tính ở nhà không cần bật.
7. Đăng nhập bằng tài khoản quản lý vừa đặt, thực hiện đổi mật khẩu khi app yêu cầu.
   Sau khi thành công, xóa `APP_ADMIN_PASSWORD` khỏi Environment trên Render và triển khai lại.
   Khi CSDL đã có tài khoản, biến khởi tạo không đặt lại mật khẩu của tài khoản cũ.
8. Quản lý cấp tài khoản cho người trong gia đình ở **Cài đặt → Quản lý tài khoản**.

Nhập số di động vào ô tên đăng nhập nếu muốn thành viên đăng nhập bằng số điện thoại
và mật khẩu. Tài khoản hiện có tiếp tục dùng thông tin đăng nhập đã được cấp.

Blueprint tắt tự triển khai khi push (`autoDeployTrigger: 'off'`). Các lần cập nhật sau:
kiểm thử → commit/push → Render **Manual Deploy → Deploy latest commit**.
Không thêm mật khẩu vào `render.yaml` hoặc Dockerfile. `sync: false` chỉ yêu cầu giá trị
khi tạo mới Blueprint; chỉnh mật khẩu về sau tại Environment của service.

## 5. Kiểm tra sau khi có địa chỉ online

- Trang `/login` mở được qua HTTPS; chưa đăng nhập không xem được vốn, nợ hoặc phiếu.
- Mỗi thành viên dùng tài khoản riêng. Cấp quyền nhân viên cho người chỉ cần bán hàng.
  Bật xác thực hai bước cho các tài khoản có quyền quản trị GitHub, Render và Neon.
- Kiểm tra nhân viên xem được vốn và phiếu chung. Quản trị tài khoản chỉ dành cho quản lý.
- Phiên hết hạn sau 30 phút không hoạt động. Login tính mọi lần gửi có CSRF hợp lệ,
  tối đa 8 lần cho cùng tên đăng nhập trong 10 phút và 120 lần cho cả web trong một phút.
  Khi đạt ngưỡng, máy chủ trả HTTP 429 và thời gian chờ trong `Retry-After`.
- Với dữ liệu thử: nhập 6 kg × 10.000 VNĐ, trả một phần; xuất bán một phần từ đúng lô;
  kiểm tra tồn, vốn, nợ và tất toán. Không tạo giao dịch thử vào dữ liệu thật của cửa hàng.
- Mở lại trên điện thoại: tiền có dấu chấm và VNĐ; ngày giờ ghi nhận theo Việt Nam.
- Đợi web ngủ rồi mở lại; chờ trang khởi động xong trước khi thao tác.

Health check dùng `/login` để kiểm tra app, không đọc CSDL liên tục. Kết nối DB rảnh được
đóng sau khoảng một phút để Neon có thể nghỉ. Health check thành công không thay kiểm tra giao dịch thật.

## 6. Giới hạn miễn phí và sao lưu

Theo [Neon Pricing](https://neon.com/pricing), Free hiện có 1 GB dữ liệu và 100 CU-hours/tháng
cho mỗi project, không giới hạn thời gian dùng gói, không yêu cầu thẻ. Vẫn cần theo dõi mức dùng
trong dashboard; miễn phí không có nghĩa tài nguyên vô hạn.

[Render Free](https://render.com/docs/free) ngủ sau 15 phút không có truy cập; lần mở tiếp theo
có thể đợi khoảng một phút. Có hạn mức giờ chạy, băng thông và build. Phù hợp thử nghiệm,
không đảm bảo luôn sẵn sàng. Không bật gói trả phí nếu chưa quyết định trả tiền.

Dữ liệu nghiệp vụ lưu ở Neon. Xem [thiết lập quyền PostgreSQL, sao lưu Drive và cảnh báo](BAO-MAT-VA-SAO-LUU.md)
để cấp quyền riêng cho web, thử khôi phục và bật lịch sao lưu.

## 7. Khi có lỗi

- **Build lỗi:** xem Render Logs, bắt đầu từ lỗi đầu tiên; không bỏ test để che lỗi.
- **Không kết nối CSDL:** đối chiếu host trực tiếp, tên database, role và mật khẩu với màn hình Connect.
  Nếu có lỗi chứng chỉ, kiểm tra đúng tên host và kho chứng chỉ của Java, giữ `verify-full` bật.
- **Login quay về trang đăng nhập:** dùng link HTTPS, kiểm tra service đang bật profile `prod`.
- **Không có tài khoản:** kiểm tra hai biến `APP_ADMIN_*` khi CSDL trống và thông báo bootstrap
  trong log; không tự chèn mật khẩu thô vào CSDL.
- **Mất phiên sau khi redeploy/ngủ:** đăng nhập lại; phiên đăng nhập nằm trong bộ nhớ của app,
  dữ liệu phiếu nằm ở Neon.
- **Web chạy nhưng không có dữ liệu cũ:** hai CSDL đang độc lập; cần kế hoạch chuyển dữ liệu,
  không tự nhập lại hoặc sao chép từng bảng tùy ý.

Tài liệu kỹ thuật: [Neon Java](https://neon.com/docs/guides/java),
[Render Docker](https://render.com/docs/docker),
[Render Blueprint](https://render.com/docs/blueprint-spec).

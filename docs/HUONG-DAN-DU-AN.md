# Hướng dẫn làm việc với dự án quản lý kho

Đọc tài liệu này khi bắt đầu mỗi phiên làm việc. Cập nhật lần gần nhất: 18/11/2026.

## 1. Thứ tự đọc trước khi sửa

1. [AGENTS.md](../AGENTS.md): quy tắc bắt buộc và nghiệp vụ đã chốt.
2. `HUONG-DAN-DU-AN.md`: mục tiêu, cách chạy và quy trình làm việc.
3. Chọn skill cho công việc trong [thư mục Codex của dự án](../.codex/README.md).
4. Đọc [thiết kế](giao-dien/DESIGN.md) và [quy ước tương tác](giao-dien/UX-CONTRACT.md) khi làm giao
   diện.
5. Đọc controller, service, template và test của chức năng liên quan.

Yêu cầu mới của người dùng được ưu tiên; không tự thay đổi nghiệp vụ vì một skill gợi ý khác. Kiểm
tra `git status --short` và nhánh hiện tại trước khi sửa. Giữ các thay đổi đang có; không tự chuyển
nhánh, reset, commit hoặc push khi chưa có yêu cầu phù hợp. Kết quả rà soát và giới hạn của đợt
chuẩn hóa gần nhất ở [báo cáo kiểm thử](kiem-thu/KIEM-TRA-CHAT-LUONG.md).

## 2. Sản phẩm dành cho ai

Web nội bộ cho 5–6 người trong gia đình bán vật liệu xây dựng và nông sản, dùng chủ yếu trên điện
thoại. Công việc chính: ghi nhập/bán, theo dõi từng lô, hao hụt, vốn tồn và công nợ. Quản lý cấp tài
khoản trong **Cài đặt → Quản lý tài khoản**. Thành viên đăng nhập bằng tên đăng nhập hoặc số điện
thoại đã được cấp cùng mật khẩu. Quản lý vẫn xử lý được các tài khoản đang chờ duyệt từ phiên bản
trước.

OWNER quản lý sản phẩm, nhập hàng, hao hụt, vốn/nợ và tài khoản. Nhập hàng chỉ cần ghi hàng, số
lượng, giá, tiền đã trả rồi lưu. Tên người bán gõ trực tiếp, không bắt buộc (tối đa 150 ký tự),
không phải tạo nhà cung cấp trước. Phiếu cũ giữ tên nhà cung cấp đã liên kết; phiếu mới không tạo
bản ghi nhà cung cấp. Mỗi phiếu mới một mặt hàng: gõ tên hàng, chọn nhóm/đơn vị gốc, ghi số lượng và
giá. Hàng chưa có được lưu tự động cùng phiếu; hàng trùng tên/nhóm/đơn vị được dùng lại. Mã lô được
tạo sẵn, có thể sửa trước khi lưu. Lưu lặp cùng mã lần gửi không nhân đôi tiền/hàng. Lịch sử nhập có
tìm người bán, tên hàng, mã lô; lịch sử xuất có tìm người nhận/mã phiếu. Trang chủ đưa vốn hàng còn,
tiền đã chi nhập và tiền đã thu bán lên trước công cụ phụ. Cài đặt ở góc phải chứa Sáng/Tối, quản lý
sản phẩm/tài khoản, đổi mật khẩu và đăng xuất. Chế độ giao diện nhớ trên trình duyệt. Nút Tất toán
trên xuất bán chọn/bỏ chọn thu đủ; chỉ ghi tiền khi lưu phiếu, bỏ chọn vẫn giữ số tiền trả một phần
đã nhập. STAFF xem tồn/giá bán, lập phiếu bán, chỉ xem phiếu do mình lập; không được lộ giá vốn.
Kiểm tra quyền tại máy chủ, không chỉ ẩn nút ở giao diện.

## 3. Bản đồ mã nguồn

| Nội dung               | Nơi sửa chính                                                                                                                                                                                      |
| ---------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Nghiệp vụ và tính toán | `src/main/java/com/vatlieu/service/`                                                                                                                                                               |
| Nhận form, định tuyến  | `src/main/java/com/vatlieu/web/`, `form/`                                                                                                                                                          |
| Dữ liệu                | `entity/`, `repository/` cùng gốc Java                                                                                                                                                             |
| Trang HTML             | Trong `src/main/resources/templates/`, nhóm theo `purchases/` (nhập), `outbound/` (xuất), `stock/` (tồn), `catalog/` (danh mục) và `account/` (tài khoản). Trang chủ và vốn nằm ở gốc thư mục này. |
| Thành phần dùng chung  | `templates/fragments.html`, `static/css/sky.css`, `static/js/`                                                                                                                                     |
| Thay đổi CSDL          | Thêm migration mới trong `src/main/resources/db/migration/`                                                                                                                                        |
| Kiểm thử               | `src/test/java/`, `src/test/js/`                                                                                                                                                                   |

Giữ Java 17, Spring Boot, Thymeleaf và Bootstrap hiện tại. Không tự chuyển sang React, thay
framework hay nâng thư viện chỉ để chỉnh giao diện.

## 4. Các quy ước phải giữ

- Tiền nhập và hiển thị: `1.234.567 VNĐ`; số lượng `6`, `6.5`, không thêm dấu phân cách nghìn.
- BigDecimal cho tiền/số lượng. Cộng toàn bộ dòng rồi làm tròn tổng phiếu HALF_UP đến đồng.
- Tồn tính từ giao dịch kho; không sửa trực tiếp số tồn. Số lượng gốc tối đa 3 số lẻ.
- Tồn bằng 0 hiện Hết hàng, kể cả mức tối thiểu là 0. Còn hàng dưới mức tối thiểu hiện Sắp hết; bằng
  mức tối thiểu chưa cảnh báo. Hàng còn và lô tồn có bộ lọc nhóm hàng.
- Giá vốn theo từng lô. Xuất bán chọn lô hoặc FIFO khi để trống; hao hụt FIFO.
- Nông sản dùng kg thực tế: tổng kg, từng bao hoặc cả lô đã biết kg và tổng tiền. Không quy đổi cố
  định bao sang kg; không có lô chờ cân.
- Xóa sản phẩm là ngừng sử dụng; giữ lịch sử, vốn và công nợ. Chức năng NCC cũ giữ để tương thích dữ
  liệu, không xuất hiện trong điều hướng chính hay form nhập hàng.
- Tất toán ghi thu/trả đủ phần nợ, có lịch sử và chống gửi trùng; không xóa khoản nợ khỏi dữ liệu.
- Vốn tồn, lãi gộp và chênh lệch thu/trả có ý nghĩa khác nhau; chưa gọi chênh lệch thu/trả là số dư
  quỹ.

## 5. Quy trình làm một chức năng

1. Xác định màn hình, đối tượng dùng, thao tác chính và kết quả sau khi lưu.
2. Đọc chức năng tương tự; tái sử dụng thành phần dùng chung.
3. Hỏi lại khi thiếu quy tắc về tiền, kho, quyền hoặc hủy/hoàn phiếu; tự quyết các chi tiết bố cục
   nhỏ.
4. Sửa trong phạm vi yêu cầu; không dùng dữ liệu thật để thử thanh toán, xóa hay xuất kho.
5. Kiểm tra trạng thái có dữ liệu, rỗng, lỗi, đang gửi, tên dài, tiền lớn, điện thoại và quyền
   STAFF.
6. Chạy test, khởi động app, xem lại diff; cập nhật tài liệu nếu quyết định dùng lâu dài thay đổi.
7. Báo ngắn gọn: file sửa, cách thử, kết quả kiểm tra và phần còn thiếu. Không chụp ảnh báo cáo nếu
   người dùng chưa yêu cầu lại.

## 6. Chạy và kiểm thử

Trên máy hiện tại: nhấp `Mo-app.cmd`, hoặc chạy `./Mo-app.cmd` trong thư mục dự án. Tệp này sử dụng
công cụ cục bộ trong `.tools/` và mở cổng **18080**. Mở **http://127.0.0.1:18080/**. PostgreSQL và
tiến trình Java cần tiếp tục chạy.

Trên máy đã cài Java/Maven/Docker, cấu hình mật khẩu bằng biến môi trường rồi chạy:

```powershell
docker compose up -d
mvn spring-boot:run
```

Cổng mặc định của cách chạy này là 8080 nếu chưa đặt SERVER_PORT. Không giả định `.tools/` và `.m2/`
có sẵn trên máy clone mới; hai thư mục này không đưa lên Git. Docker Compose và Spring cùng đọc
`SPRING_DATASOURCE_PASSWORD`; phải đặt biến này trước khi chạy. `Mo-app.cmd` trên máy hiện tại tự
đọc mật khẩu cục bộ đã có. Nếu CSDL đã tồn tại, dùng đúng mật khẩu của CSDL; đổi biến môi trường
không đổi mật khẩu CSDL.

```powershell
mvn test
node --test src/test/js/*.test.cjs
```

Test PostgreSQL cần `RUN_POSTGRES_TESTS=true` và các biến `SPRING_DATASOURCE_*` trỏ tới CSDL kiểm
thử riêng. Không chạy test tích hợp trên CSDL cửa hàng. Kiểm tra số test bị bỏ qua; test qua khi bỏ
qua tích hợp không có nghĩa đã kiểm tra CSDL. Không ghi mật khẩu vào Markdown, log, Git hoặc ảnh
chụp.

### Format mã nguồn

Node.js chỉ dùng cho công cụ phát triển, không cần để chạy ứng dụng Java. Dùng Node 24.15 trở lên
trong nhánh 24 LTS (máy hiện tại 24.16) hoặc phiên bản tương thích với `jsdom` trong
package-lock.json.

```powershell
npm ci
npm run format
npm run format:check
npm test
```

Prettier và plugin Java/XML khóa phiên bản trong package-lock.json; cấu hình ở `.prettierrc.json`,
editor ở `.editorconfig`. Format Java, template HTML, CSS, JS và pom.xml; không đổi migration, dữ
liệu hay thư viện chạy app. Khi sửa nhỏ, format riêng tệp liên quan. Nguồn công cụ:
[Prettier](https://prettier.io/docs/configuration),
[plugin Java](https://github.com/jhipster/prettier-java).

## 7. Skill sử dụng

Các skill frontend, backend, database và API nằm trong [mục lục của dự án](../.codex/README.md).
Bảng dưới liệt kê những skill bổ sung có thể dùng nếu đã cài trên máy.

| Skill                                     | Khi áp dụng                                                                                                                            |
| ----------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| frontend-design + frontend-design-premium | Thiết kế/chỉnh giao diện; đọc cả hai, rồi DESIGN và UX-CONTRACT                                                                        |
| computer-use                              | Kiểm tra trực tiếp giao diện trong môi trường Codex có công cụ này                                                                     |
| playwright                                | Viết/chạy kiểm thử trình duyệt khi môi trường cho phép; không thay công cụ điều khiển bắt buộc của phiên                               |
| webapp-testing                            | Kiểm tra DOM đã tải, thao tác theo nhãn, đối chiếu kết quả và log; chỉ ghi giao dịch vào CSDL thử riêng                                |
| gh-fix-ci                                 | Điều tra lỗi kiểm tra tự động trên GitHub khi cần                                                                                      |
| security-threat-model                     | Rà rủi ro khi thêm quyền, dữ liệu nhạy cảm hoặc triển khai Internet                                                                    |
| security-best-practices                   | Kiểm tra JavaScript trên trình duyệt trước khi đưa web lên mạng. Với Java và PostgreSQL, đối chiếu tài liệu Spring Security và pgJDBC. |
| skill-installer                           | Cài skill còn thiếu từ nguồn tin cậy; không tải lại skill đã có                                                                        |
| vatlieu-code-quality                      | Format và kiểm tra kiến trúc/chức năng theo quy ước riêng của dự án                                                                    |

Hai skill frontend đã có trên máy. Ba skill playwright, gh-fix-ci, security-threat-model được cài từ
`openai/skills` ngày 09/11/2026. Skill nằm ở cấu hình Codex của máy, không nằm trong repository; máy
khác cần cài riêng. Không tự tạo CI/triển khai chỉ vì đã cài skill. Skill `vatlieu-code-quality`
được tạo riêng trên máy; quy tắc format và lệnh kiểm tra vẫn lưu trong repository để máy khác có thể
dùng mà không phụ thuộc skill. Đã cài thêm `webapp-testing` từ
[anthropics/skills](https://github.com/anthropics/skills/tree/main/skills/webapp-testing) ngày
09/11/2026 vào `C:/Users/user/.codex/skills/webapp-testing`. Skill mới có trong lượt làm việc tiếp
theo; trong lượt cài đã đọc và áp dụng quy trình kiểm tra. Không đưa skill, CSDL thử, mật khẩu hoặc
log cục bộ vào Git. Đây là hướng dẫn kiểm thử, không phải chứng nhận web đạt mọi tiêu chuẩn. Kết quả
cần tách rõ đã đạt, nhận diện nhầm và chưa kiểm tra.

## 8. Lộ trình và phần chưa có

Ưu tiên: giao diện dễ thao tác → sao lưu/khôi phục được kiểm chứng → sửa/hủy/hoàn phiếu → quỹ đầu
kỳ, góp/rút vốn, chi phí → báo cáo theo kỳ, kiểm kê → triển khai khi có nhu cầu. Sao lưu mới được
trao đổi, chưa có script sao lưu tự động. GitHub lưu mã nguồn, không thay bản sao CSDL. Giá sỉ theo
bậc, báo cáo kỳ, kiểm kê, sửa/hủy/hoàn phiếu và triển khai Internet chưa được coi là hoàn thành.

Đã chuẩn bị Dockerfile, `render.yaml` và profile `prod` cho Neon Free + Render Free. Xem
[hướng dẫn triển khai](trien-khai/TRIEN-KHAI-NEON-RENDER.md) trước khi triển khai; chưa coi là
online nếu chưa kiểm tra URL thật. Không đưa bí mật/CSDL vào Git hoặc image. Chuyển dữ liệu cửa hàng
cần xác nhận riêng; không chạy test tích hợp trên Neon đang dùng thật. Bản cloud và bản local chưa
tự đồng bộ.

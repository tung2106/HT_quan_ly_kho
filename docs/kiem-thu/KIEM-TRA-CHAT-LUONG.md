# Kiểm tra chất lượng — 09/11/2026

## Phạm vi thay đổi

- Chuẩn hóa format Java, HTML, CSS, JavaScript và pom.xml bằng Prettier khóa phiên bản.
- Giữ cấu trúc Spring Boot hiện có. Ngoài bốn lớp Product/Supplier service và controller
  được chỉnh bên dưới, các tệp Java ứng dụng chỉ đổi format, import và ngoặc tương đương.
  Giữ phép tính tiền, SQL nhập/xuất, lô, tồn kho và thanh toán.
- Chuyển thêm NCC và thêm/xóa quy đổi vào service có transaction và kiểm tra quyền.
  Thêm NCC dùng cùng validation với sửa; quy đổi chặn tên rỗng/dài, hệ số không dương,
  tên trùng không phân biệt hoa thường và xóa nhầm quy đổi thuộc sản phẩm khác.
- Trang chủ chia nhóm ghi giao dịch, hàng hóa, tra cứu và tài khoản; dùng chung icon,
  thanh điều hướng, màu sắc, kích thước nút và khung nội dung.
- Bổ sung nhãn ô nhập, thông báo lỗi tại ô, nút xóa tìm kiếm, hiện/ẩn mật khẩu,
  hộp xác nhận có Hủy/Escape; dòng sao chép có ID riêng và không mang lỗi dòng cũ.
- Bỏ mật khẩu ghi trực tiếp trong cấu hình app/Docker; đọc SPRING_DATASOURCE_PASSWORD.
  Launcher hiện tại vẫn đọc bí mật cục bộ, không đổi mật khẩu CSDL.
- Không sửa migration hoặc ghi giao dịch thử lên CSDL cửa hàng.

## Kết quả kiểm tra

| Kiểm tra | Kết quả |
|---|---|
| `npm run format:check` | Đạt |
| `npm test` | 11 test đạt, không bỏ qua |
| `mvn test`, RUN_POSTGRES_TESTS=true | 42 test đạt, không bỏ qua |
| `mvn -DskipTests package` | Tạo JAR thành công sau khi tải plugin đóng gói thiếu trong cache |
| Khởi động JAR | Cổng thử 18081, CSDL riêng; HTTP /login = 200; đã dừng tiến trình thử |
| Web đang sử dụng | Đã khởi động lại sau sửa service, cổng 18080 trả HTTP 200 |
| Lint DESIGN.md | 0 lỗi, 0 cảnh báo |
| Kiểm tra skill vatlieu-code-quality | Hợp lệ |
| `git diff --check` | Đạt |

Test tích hợp dùng CSDL riêng `vatlieu_quality_20261109`, được tạo cho đợt kiểm tra này.
Log cục bộ trong `.tools/quality-tests.log`, `.tools/quality-build.log`,
`.tools/quality-startup.log`; `.tools/` không đưa lên Git.

Các trường hợp đã chạy gồm: cộng tiền rồi làm tròn; số lẻ; nhiều lô giá khác nhau;
chọn lô; bán đúng/vượt tồn; hai người bán đồng thời; quy đổi; nhập nông sản từng bao/cả lô;
thu trả từng phần/vượt nợ; tất toán và gửi trùng; xóa/khôi phục NCC; phân quyền và CSRF.
Test trang chủ được cập nhật để kiểm tra đường dẫn thao tác thay vì phụ thuộc tiêu đề cũ.
Test thêm NCC kiểm tra khoảng trắng, tên rỗng/quá dài và điện thoại quá dài. Test quy đổi
kiểm tra hệ số lẻ, 0/âm, tên trùng hoa thường, chặn quy đổi bao nông sản, xóa sai và đúng sản phẩm.

## Kiểm tra trực tiếp trên trình duyệt

- 360px: trang chủ, nhập hàng, tồn kho, sản phẩm, công nợ, lịch sử thu/trả không tràn ngang trang.
  Bảng tồn kho cuộn trong khung riêng, giữ đủ các cột.
- Nhập `123456789012` ở đơn giá thành `123.456.789.012`, hậu tố VNĐ riêng;
  số lượng `6` vẫn là `6`. Thêm dòng không trùng ID và không thiếu nhãn ô đang hiển thị.
- Mở chọn nông sản, chuyển nhóm và chọn sản phẩm; các trường kg/giá và đơn vị cập nhật.
- Mở hộp xóa sản phẩm rồi Escape: không gửi thao tác, trả focus về nút Xóa.
- Đổi tab công nợ bằng bàn phím, chỉ một danh sách hiện; tìm không thấy và xóa tìm kiếm.
- Công nợ và hai màn hình lịch sử mỗi trang một h1.
- 1280px: trang chủ giới hạn nội dung 1080px, không tràn ngang.
- Giữ web mở để người dùng theo dõi; không chụp ảnh báo cáo.

Audit tĩnh của skill lưu ở `.tools/ui-audit.json`: còn 2 báo cáo
`affordance.actionless-button` tại hai tab công nợ. Đây là nhận diện nhầm: handler
được gắn bằng `addEventListener` trong `debt-tabs.js`, không phải `onclick` trong HTML.
Đã xác minh bằng test DOM và thao tác bàn phím thực tế. Không sửa auditor hoặc thêm
handler trùng chỉ để làm báo cáo xanh; không coi kết quả strict audit này là đã đạt.

## Nhận xét kiến trúc và phần chưa làm

- Tính tiền, kho, lô, thanh toán và phân quyền tiếp tục nằm ở các service hiện có;
  controller nhập/xuất điều phối form và phản hồi. JS chỉ hỗ trợ nhập liệu.
- Đã chuyển hai đường ghi cũ ở thêm NCC và quy đổi đơn vị từ controller sang service.
  Controller chỉ gọi service, nhận lỗi hợp lệ và điều hướng; không thêm tầng trung gian.
- Danh sách chưa phân trang máy chủ; chưa khôi phục bản nháp khi hết phiên,
  chưa cảnh báo rời form có dữ liệu chưa lưu. Chưa kiểm thử trên thiết bị Android/iOS thật,
  mất mạng, Safari hoặc toàn bộ trạng thái trợ năng.
- Chưa thêm nghiệp vụ mới, sao lưu tự động, CI hoặc triển khai Internet.
  Chưa commit/push. Thay đổi có sẵn ở Mo-app.ps1 được giữ nguyên.

## Tệp tạo/sửa

- Tài liệu: AGENTS.md, README.md, HUONG-DAN-DU-AN.md, DESIGN.md, UX-CONTRACT.md,
  KIEM-TRA-CHAT-LUONG.md.
- Công cụ: package.json, package-lock.json, .prettierrc.json, .prettierignore,
  .editorconfig, premium-ui.json, .gitignore, pom.xml.
- Cấu hình: docker-compose.yml, src/main/resources/application.properties.
- Format: các tệp Java trong src/main/java và src/test/java, template trong
  src/main/resources/templates và script trong src/main/resources/static/js.
- Giao diện dùng chung: static/css/sky.css, static/js/ui.js, static/js/loading.js,
  static/images/ui-icons.svg; các template trang chủ, tồn kho, biểu mẫu và xác nhận.
- Test bổ sung/cập nhật: src/test/js/ui.test.cjs,
  src/test/java/com/vatlieu/AccessPostgresTest.java, PurchasePostgresTest.java.
- Skill trên máy: ~/.codex/skills/vatlieu-code-quality/SKILL.md; cài thêm playwright,
  gh-fix-ci, security-threat-model từ openai/skills. Mở phiên Codex mới để nhận skill mới.

## Cập nhật 09/11/2026 — Nhập hàng trực tiếp

Theo quyết định của người dùng: không cần tạo/chọn nhà cung cấp, chỉ ghi tên người bán
trực tiếp nếu muốn. Bỏ mục NCC trên trang chủ và tồn kho; giữ mèo ở trang chủ.
Tên tối đa 150 ký tự, bỏ khoảng trắng hai đầu, bỏ trống vẫn lưu được. Tên được dùng
ở phiếu nhập, lô tồn, công nợ và lịch sử trả; không tạo bản ghi NCC mới từ tên này.
Phiếu cũ giữ liên kết/tên NCC. Công nợ vẫn theo từng phiếu, trả một phần hoặc tất toán.

### Tệp tạo/sửa trong lần này

- Mới: `src/main/resources/db/migration/V6__purchase_seller_name.sql`.
- Lưu và đọc dữ liệu: `src/main/java/com/vatlieu/entity/PurchaseOrder.java`,
  `form/PurchaseForm.java`, `service/PurchaseService.java`, `service/OperationsService.java`,
  `service/StockService.java`, `web/PurchaseController.java` (cùng gốc Java trên).
- Giao diện trong `src/main/resources/templates/`: `home.html`, `stock.html`,
  `purchase-form.html`, `purchases.html`, `capital.html`, `stock-lots.html`.
- Test trong `src/test/java/com/vatlieu/`: `PurchasePostgresTest.java`,
  `OperationsPostgresTest.java`, `service/PurchaseServiceTest.java`, `web/PurchaseTemplateTest.java`.
- Tài liệu: `AGENTS.md`, `HUONG-DAN-DU-AN.md`, `DESIGN.md`, `UX-CONTRACT.md`, tài liệu này.

### Kiểm tra và cách thử

- Maven `test package`: 48 test đạt, 0 bỏ qua; dùng PostgreSQL riêng
  `vatlieu_quality_20261109`, không chạy test trên CSDL cửa hàng.
- `npm test`: 11 test đạt. `npm run format:check` và `git diff --check` đạt.
- Lint DESIGN: 0 lỗi/cảnh báo. Audit `.tools/direct-purchase-audit.json` còn đúng hai
  nhận diện nhầm tab công nợ đã mô tả ở trên, không phát sinh lỗi mới.
- Test gồm tên trống/chỉ khoảng trắng, 150/151 ký tự, tên có dấu và ký tự HTML,
  giữ nội dung khi lỗi, trả một phần/tất toán, lịch sử và công nợ của NCC đã ẩn.
- Sửa tìm lịch sử theo hoa/thường tiếng Việt bằng Unicode ở Java để không phụ thuộc
  locale PostgreSQL; tìm chuỗi con vẫn coi `%` và `_` là ký tự bình thường.
- Trình duyệt, CSDL tạm riêng: nhập 6 kg × 12.345 VNĐ, trả 10.000 VNĐ → tổng 74.070 VNĐ,
  còn nợ 64.070 VNĐ. Tên xuất hiện trên phiếu/công nợ/lịch sử; tìm “anh” trong công nợ,
  tìm “cô ánh” trong lịch sử trả đều ra “Cô Ánh”. Lỗi thiếu hàng giữ tên đã nhập.
- Viewport 360/390px: form nhập, phiếu, công nợ và lịch sử trả không tràn ngang.
  Không chụp ảnh báo cáo. Đã tắt tiến trình, xóa CSDL preview tạm và khôi phục viewport.
- App cổng 18080 khởi động lại thành công; Flyway V6 chạy, `/login` trả HTTP 200.

Mở `/purchases/new`, chọn hàng, ghi số lượng/giá/số tiền đã trả rồi lưu; tên người bán
có thể bỏ trống. Vào Vốn / Nợ → Công nợ → Trả tiền nhập hàng để trả phần còn thiếu.
Chưa kiểm tra trên điện thoại vật lý; chưa commit/push. Chức năng NCC cũ còn ở máy chủ
để tương thích dữ liệu, không còn trong luồng nhập chính. Danh sách lịch sử vẫn chưa
phân trang; khi bổ sung phân trang cần đưa lọc Unicode vào truy vấn trước khi phân trang.

## Cập nhật 09/11/2026 — Một lần nhập, một mặt hàng và một mã lô

### Cách sử dụng đã chốt

- Vào Nhập hàng, gõ tên hàng; hàng mới được lưu cùng phiếu, không cần tạo sản phẩm
  hoặc nhà cung cấp trước. Mỗi lần nhập một mặt hàng. Chọn nhóm hàng và đơn vị gốc;
  hàng đã có được gợi ý lại nhóm/đơn vị khi gõ đúng tên.
- Mã lô tự tạo dạng `NH-000001`, có thể sửa trước khi lưu. Mã phải duy nhất;
  mở form rồi bỏ có thể làm số thứ tự bị ngắt, không có nghĩa mất phiếu.
- Tên người bán không bắt buộc. Nhập lượng, tiền mua, tiền đã trả hoặc chọn trả đủ.
  Nông sản giữ ba cách nhập: tổng kg, kg từng bao, tổng kg và tổng tiền lô.
- Khi xuất bán, ghi người nhận, chọn hàng/lô, số lượng, giá bán và tiền đã thu.
  Để trống lô thì FIFO; khi còn nợ phải có tên người nhận.
- Trang chủ giữ mèo, hiện vốn trong hàng còn, tiền đã chi nhập và tiền đã thu bán.
  Có lối vào lịch sử nhập/bán và tìm tên không dấu; xem từng lô để biết mua của ai,
  còn bao nhiêu và giá vốn riêng.

### Tệp tạo/sửa trong lần này

- Mới: `src/main/resources/db/migration/V7__purchase_lot_code_and_request_key.sql`,
  `src/main/java/com/vatlieu/service/SearchText.java`,
  `src/test/java/com/vatlieu/DirectPurchasePostgresTest.java`,
  `src/test/js/purchase-entry.test.cjs`.
- Java trong `src/main/java/com/vatlieu/`: `entity/PurchaseOrder.java`,
  `form/PurchaseForm.java`, `service/ProductService.java`, `service/PurchaseService.java`,
  `service/OperationsService.java`, `service/StockService.java`, `service/SupplierService.java`,
  `web/PurchaseController.java`, `web/OperationsController.java`, `web/HomeController.java`.
- Template trong `src/main/resources/templates/`: `purchase-form.html`, `purchases.html`,
  `outbound-form.html`, `outbound.html`, `outbound-detail.html`, `home.html`, `stock.html`,
  `stock-lots.html`, `fragments.html`.
- Giao diện: `src/main/resources/static/js/purchase-entry.js`,
  `src/main/resources/static/css/sky.css`.
- Test cập nhật trong `src/test/java/com/vatlieu/`: `PurchasePostgresTest.java`,
  `service/PurchaseServiceTest.java`, `web/PurchaseTemplateTest.java`.
- Tài liệu: `AGENTS.md`, `HUONG-DAN-DU-AN.md`, `DESIGN.md`, `UX-CONTRACT.md`, tài liệu này.

### Kết quả kiểm tra

- `mvn test`: 53 test đạt, không bỏ qua; PostgreSQL kiểm thử riêng
  `vatlieu_quality_20261109`. `npm test`: 13 test đạt. Package tạo thành công.
  Log ở `.tools/simple-ledger-tests.log`, `.tools/simple-ledger-package.log`.
- Kiểm tra format và `git diff --check` đạt. Lint DESIGN không lỗi/cảnh báo.
  Strict audit `.tools/simple-ledger-audit.json` vẫn còn hai nhận diện nhầm nút tab
  công nợ đã giải thích ở trên; không coi strict audit là đạt.
- Test mới kiểm tra cùng tên dùng lại sản phẩm nhưng mỗi lần mua có giá/lô riêng;
  bán đúng lô đã chọn; phiếu gửi lại không cộng hàng/tiền hai lần; tiền lẻ làm tròn;
  mã lô trùng; sản phẩm đã ngừng dùng; chặn nhiều mặt hàng trong form trực tiếp.
  Giao dịch nhập lỗi hoàn tác cả sản phẩm mới, không để lại sản phẩm rác.
- Browser với CSDL tạm riêng: nhập Sắn tươi lô `SAN-01`, 1000 kg × 2000 VNĐ,
  trả 500000 VNĐ; nhập tiếp cùng hàng 500 kg × 3000 VNĐ và trả đủ.
  Bán 200 kg từ `SAN-01` × 4000 VNĐ, thu 500000 VNĐ:
  giá vốn xuất 400000 VNĐ, nợ khách 300000 VNĐ.
- Trang chủ đúng vốn còn 3.100.000 VNĐ, đã chi 2.000.000 VNĐ, đã thu 500.000 VNĐ.
  Lô đầu còn 800 kg, vốn 1.600.000 VNĐ; lô sau còn 500 kg, vốn 1.500.000 VNĐ.
  Lịch sử tìm `anh` thấy Cô Ánh, tìm `tuan` thấy Anh Tuấn.
- Viewport 360px: form nhập, trang chủ và lô tồn không tràn ngang; số lượng hiện
  1000/800/500, tiền có dấu chấm và VNĐ. Mèo trang chủ tải thành công.
  Không chụp ảnh báo cáo. Đã khôi phục viewport, đóng tab/tiến trình và xóa CSDL preview.
- App chính cổng 18080 đã khởi động, Flyway V7 thành công; `/login` HTTP 200.
  Không tạo phiếu thử vào CSDL cửa hàng.

### Phần chưa làm

Chưa commit/push, triển khai Internet hoặc kiểm tra điện thoại vật lý.
Danh sách lịch sử chưa phân trang. Form nhập gọn dùng đơn vị gốc; phần quy đổi
vật liệu và phiếu nhiều dòng cũ vẫn giữ tương thích ở nghiệp vụ, không đưa lại vào form mới.
Các thay đổi có sẵn trước lần này được giữ nguyên.

## Cập nhật 09/11/2026 — Cài đặt, sáng/tối và nút tất toán xuất bán

### Thay đổi và cách thử

- Góc phải có Cài đặt dùng chung: Sáng/Tối; Sản phẩm và Quản lý tài khoản cho OWNER;
  Đổi mật khẩu và Đăng xuất. Giữ lối vào nhập, bán, hàng còn, vốn/nợ và lịch sử ở ngoài.
  Hao hụt vẫn là thao tác hàng hóa. Mèo ở trang chủ và màn hình chờ được giữ nguyên.
- Sửa/thêm/ngừng sản phẩm qua Cài đặt → Sản phẩm. Nhân viên vẫn xem giá bán từ Hàng còn;
  không thấy công cụ quản lý. Quyền và CSRF máy chủ giữ nguyên.
- Chọn Sáng hoặc Tối: áp dụng ngay, giữ khi chuyển trang/tải lại và giữa các tab cùng nguồn.
  Lưu duy nhất tên theme trên trình duyệt; bị chặn lưu trữ vẫn đổi được trong trang hiện tại.
- Đóng Cài đặt bằng nút Đóng, Escape hoặc chạm nền bên ngoài. Chạm bên trong hoặc kéo
  từ trong ra ngoài không đóng nhầm. Trả focus về Cài đặt, giữ nội dung form đang nhập.
- Xuất bán dùng nút Tất toán rộng cả hàng, trạng thái có màu và chữ. Bấm lần nữa bỏ chọn,
  khôi phục ô tiền trả một phần cùng giá trị đã nhập. Nút này không gửi phiếu;
  chỉ Lưu và trừ kho mới ghi tiền. Phiếu nhập vẫn dùng ô tích như trước.
- Trang đang mở cần tải lại một lần để nhận JavaScript mới.

### Tệp tạo/sửa

- Mới: `src/main/resources/static/js/theme.js`, `src/test/js/theme.test.cjs`,
  `src/test/js/settlement.test.cjs`.
- Giao diện: `src/main/resources/templates/fragments.html`, `home.html`, `stock.html`,
  `password.html`, `outbound-form.html`, `purchase-form.html` (cùng thư mục templates).
- Dùng chung: `src/main/resources/static/css/sky.css`, `static/js/ui.js`,
  `static/js/back.js`, `static/js/settlement.js`, `static/images/ui-icons.svg`
  (các đường static cùng gốc `src/main/resources/`).
- Test: `src/test/js/ui.test.cjs`, `src/test/java/com/vatlieu/AccessPostgresTest.java`.
- Tài liệu: `AGENTS.md`, `DESIGN.md`, `UX-CONTRACT.md`, `HUONG-DAN-DU-AN.md`, tài liệu này.
  Không sửa nghiệp vụ tính tiền, migration hoặc dữ liệu cửa hàng.

### Kiểm tra đã chạy

- `mvn test package`: 53 test Java đạt, không bỏ qua, dùng CSDL riêng
  `vatlieu_quality_20261109`; log `.tools/settings-tests.log`.
  Test phân quyền kiểm tra mục quản lý bị ẩn với STAFF và logout có CSRF.
- `npm test`: 20 test đạt, gồm trạng thái tất toán, dữ liệu gửi, giữ tiền khi bỏ chọn,
  ô tích nhập hàng cũ, ghi nhớ theme, đồng bộ tab, lưu trữ bị chặn, đóng menu và focus.
  Đã chạy lại toàn bộ JS sau khi bổ sung chạm bên ngoài để đóng.
- Format đạt; `git diff --check` đạt; lint DESIGN có 0 lỗi/cảnh báo.
  Đóng gói cuối sau sửa JS đạt; log `.tools/settings-final-package.log`.
- JAR khởi động ở 18081 với CSDL kiểm thử, `/login` HTTP 200; đã tắt tiến trình thử.
  App cửa hàng ở 18080 vẫn trả HTTP 200. Log `.tools/settings-startup.log`.
- Browser 360px: trang chủ, vốn, thanh Trang chủ/Danh mục/Cài đặt và form xuất không tràn ngang.
  Nút Tất toán rộng khoảng 317px, cao khoảng 48px; dùng Space chọn/bỏ chọn được.
  Tiền `1.234.567` được giữ sau khi bỏ tất toán và đổi theme; hậu tố VNĐ còn nguyên.
- Browser 390px: nhập hàng không tràn ngang, chữ sáng trên nền ô tối; danh sách đơn vị
  native mở bằng Alt+Down. Theme tối giữ khi chuyển trang; mèo tải thành công.
- Menu mở focus vào Đóng, Escape trả về Cài đặt. Kiểm tra trực tiếp ở tab tạm:
  bấm nền bên ngoài đóng khung, không kích hoạt đường dẫn phía dưới, không gửi form.
  Đã khôi phục viewport; không chụp ảnh báo cáo và không gửi phiếu thử vào dữ liệu thật.

Audit `.tools/settings-ui-audit.json` báo 7 `affordance.actionless-button`: 2 tab công nợ,
4 nút mở/đóng/chọn theme và nút tất toán. Đây là hạn chế dò sự kiện tĩnh: hành vi gắn bằng
`addEventListener` đã được kiểm tra bằng test và trình duyệt. Không thêm handler trùng hoặc
sửa auditor để né báo cáo; không coi strict audit này là đạt.

Chưa thử Android/iOS thật, Safari hoặc toàn bộ ma trận trợ năng; chưa commit/push.
Lựa chọn theme lưu theo trình duyệt, chưa đồng bộ theo tài khoản giữa các thiết bị.

## Cập nhật 09/11/2026 — Bộ lọc lô, cảnh báo hết hàng và kiểm thử web

### Lỗi xác nhận và xử lý

| Phát hiện | Nguyên nhân và cách sửa | Kết quả |
|---|---|---|
| Cát đã bán hết nhưng không cảnh báo | Điều kiện cũ là tồn nhỏ hơn mức tối thiểu: 0 không nhỏ hơn 0. Thêm trạng thái Hết hàng khi tồn không dương; Sắp hết chỉ dùng khi còn hàng dưới mức tối thiểu. | Test và trình duyệt đạt; không sửa số tồn hoặc mức tối thiểu trong CSDL cửa hàng. |
| Một số trang thiếu vùng nội dung chính | Đổi khung ngoài cùng từ div thành main trong 7 template, giữ trường form và hành vi. | Mọi trang đầy đủ có một main; kiểm tra lại trang Hàng còn trên trình duyệt sau đóng gói. |
| stock-lots.html chưa đúng format sau sửa fragment | Chạy Prettier riêng tệp này. | format:check đạt toàn dự án. |

Theo yêu cầu phân loại: thêm bộ lọc Tất cả / Vật liệu xây dựng / Nông sản cho Hàng còn
và Các lô còn tồn. Link từ bảng tồn sang lô giữ nhóm đã chọn. Mỗi sản phẩm một tiêu đề,
gom theo ID để hai sản phẩm trùng tên không bị trộn; mỗi lần nhập bên dưới có mã lô riêng.
Tham số nhóm không hợp lệ trở về Tất cả, SQL dùng tham số. Không sửa migration.

### Skill và tiêu chí sử dụng

Đã cài `webapp-testing` từ [kho skill Anthropic](https://github.com/anthropics/skills/tree/main/skills/webapp-testing)
vào `C:/Users/user/.codex/skills/webapp-testing`; Playwright đã có sẵn nên không cài lại.
Áp dụng quy trình quan sát DOM đã tải → thao tác theo nhãn → kiểm tra kết quả/log.
Điều khiển trình duyệt bằng công cụ CUA theo quy định môi trường hiện tại.

Rà theo AGENTS, HUONG-DAN-DU-AN, DESIGN và UX-CONTRACT: phép tính và quyền máy chủ,
format mã, vùng nội dung/tiêu đề, nhãn ô nhập, kích thước nút, số tiền/số lượng,
chế độ sáng/tối, trạng thái rỗng/lỗi và bố cục điện thoại. Đây là kiểm tra chức năng
và trợ năng cơ bản; không phải chứng nhận WCAG hoặc đánh giá bảo mật toàn diện.

### Kết quả sau sửa

- `mvn test package`: 55 test đạt, 0 lỗi, 0 bỏ qua; PostgreSQL riêng
  `vatlieu_quality_20261109`. Log `.tools/web-standards-tests.log`.
- `npm test`: 20 test đạt; `npm run format:check` và `git diff --check` đạt.
- Đóng gói lại sau format: `.tools/web-standards-package.log`, BUILD SUCCESS.
  JAR khởi động được ở 18081; `.tools/web-standards-startup.log`.
  App cửa hàng 18080 trả `/login` HTTP 200; template cache tắt và tài nguyên đã cập nhật.
- Test mới: tồn 0 với mức tối thiểu 0 hoặc dương; tồn bằng/dưới mức tối thiểu;
  bán hết hàng; sản phẩm ngừng dùng hết hàng không hiện; lọc từng nhóm/nhóm lạ;
  sản phẩm cùng tên không trộn lô; nhiều lô một sản phẩm; STAFF không thấy giá vốn.
- Bộ test hiện có tiếp tục kiểm tra số lẻ/làm tròn tổng phiếu, FIFO/chọn lô,
  nhiều giá vốn, bán đúng/vượt tồn, cạnh tranh xuất kho, nông sản ba cách nhập,
  trả một phần/tất toán/gửi trùng, quyền OWNER/STAFF và CSRF.

### Kiểm tra trực tiếp bằng dữ liệu thử riêng

- 360px: trang chủ, tồn kho, lô, lịch sử nhập, form nhập, form bán, chi tiết bán,
  công nợ, lịch sử thu/trả, sản phẩm và form sửa không tràn ngang trang.
  Bảng kho cuộn trong khung. Các màn hình được rà có một h1, không trùng ID,
  ô nhập hiển thị có nhãn; nhóm nút/select/summary được đo cao ít nhất 44px.
- Có dữ liệu và trạng thái rỗng; nộp form nhập thiếu hàng hiện lỗi và focus đúng ô.
  Tiền lớn hiện `123.456.789.012` với VNĐ, mức tồn tối thiểu nhập `6` vẫn là `6`.
- Nhập Ngô QA: 6 kg × 12.345 VNĐ + cước 1.000 VNĐ = 75.070 VNĐ;
  trả 10.000 VNĐ còn 65.070 VNĐ. Tìm `anh` thấy Cô Ánh QA trong công nợ.
  Tất toán phần còn lại lưu lịch sử và ẩn phiếu khỏi danh sách còn nợ.
- Bán hết đúng lô: 6 kg × 20.000 VNĐ, chọn Tất toán → thu 120.000 VNĐ,
  nợ 0 VNĐ, giá vốn 75.070 VNĐ. Trang chủ phản ánh đúng tiền thu/chi;
  Ngô QA hiện Hết hàng và không còn trong Các lô còn tồn.
- Lịch sử trả có đủ khoản trả đầu và tất toán; lọc cùng ngày và tên hoa/thường đúng.
  Lịch sử thu có khoản 120.000 VNĐ gắn đúng khách và phiếu.
- Sáng/Tối hoạt động, giữ qua chuyển trang; Escape đóng cài đặt và trả focus.
  Mèo trang chủ tải bình thường. Không ghi nhận lỗi/warning console ở các bước đã kiểm tra.
- Sau sửa main: đã xác minh lại Hàng còn tại 360px và lọc lô nông sản.
  Lượt kiểm tra bổ sung 390px bị gián đoạn do công cụ trình duyệt, không tính là đạt.
  Đã trả viewport về mặc định, đóng tab thử, dừng tiến trình thử và xóa CSDL tạm
  `vatlieu_stock_preview_20261109`. Không tạo giao dịch thử trên CSDL cửa hàng.

Audit tĩnh `.tools/web-standards-audit.json` còn 7 `affordance.actionless-button`
như đợt Cài đặt: hai tab nợ, bốn nút cài đặt/theme, một nút tất toán.
Các sự kiện gắn bằng addEventListener đã có test/hành vi thực tế; đây là nhận diện nhầm,
không bỏ chức năng hay gắn handler trùng để né auditor. Strict audit vẫn được ghi là chưa đạt.

### Tệp sửa và cách thử

- Java: `src/main/java/com/vatlieu/service/StockService.java`, `web/HomeController.java`.
- Template: `fragments.html`, `stock.html`, `stock-lots.html`, `purchase-form.html`,
  `purchases.html`, `products.html`, `product-form.html`, `categories.html`, `suppliers.html`
  trong `src/main/resources/templates/`.
- Test: `src/test/java/com/vatlieu/OperationsPostgresTest.java`, `AccessPostgresTest.java`.
- Tài liệu: `AGENTS.md`, `HUONG-DAN-DU-AN.md`, `UX-CONTRACT.md`, tài liệu này.

Tải lại trang `http://127.0.0.1:18080/stock`: chọn nhóm rồi Lọc; mở Các lô còn tồn
để xem lô thuộc nhóm. Hàng đã hết có nhãn Hết hàng dù mức tối thiểu là 0.

Chưa kiểm tra điện thoại Android/iOS thật, Safari, tải lớn và toàn bộ ma trận trợ năng.
Bootstrap CSS hiện lấy từ CDN nên chưa bảo đảm giao diện khi mất Internet và chưa có cache.
Phân trang, khôi phục bản nháp, sao lưu tự động và CI là việc phát triển tiếp, chưa làm
trong đợt sửa lỗi này. Chưa commit/push; giữ các thay đổi có sẵn trên main.

## Chuẩn bị Neon Free + Render Free — 16/11/2026

Đã tạo Dockerfile nhiều giai đoạn (Java 17), `.dockerignore` chỉ cho mã nguồn cần thiết
vào build, `render.yaml` chỉ tạo Web Service Free, và `application-prod.properties`.
Profile prod dùng biến môi trường Neon, pool tối đa 5 kết nối, đóng kết nối rảnh,
cookie Secure/HttpOnly/SameSite=Lax, xử lý HTTPS qua proxy và múi giờ Việt Nam.
Cấu hình local không đổi. Hướng dẫn ở `TRIEN-KHAI-NEON-RENDER.md`.

Lỗi thực tế phát hiện khi chạy JAR: POST thiếu CSRF bị chặn, nhưng forward đến
`/error/403` chỉ nhận GET nên hiện lỗi 405. Đã cho trang lỗi nhận mọi phương thức;
request thiếu CSRF nay trả 403. Không tắt kiểm tra CSRF hoặc thay quyền truy cập.

Kết quả:

- `mvn test package` với CSDL riêng `vatlieu_quality_20261109`: 55 test,
  không lỗi, không bỏ qua. Bao gồm số lẻ, nhiều lô, vượt tồn, công nợ và quyền truy cập.
- `npm test`: 20 test qua. Prettier kiểm tra các file mới có parser hỗ trợ;
  `git diff --check` không báo lỗi khoảng trắng.
- Đóng gói khi không có thông tin kết nối DB cũng thành công; test PostgreSQL chỉ
  bật khi chỉ định CSDL thử, không chạy trong bước build Docker.
- Khởi động JAR bằng profile prod trên cổng thử 18081, heap 300 MB, với CSDL trống
  được tạo riêng: 7 migration thành công, bootstrap đúng một quản lý.
- Kiểm tra HTTP với header mô phỏng proxy HTTPS: cookie có đủ ba thuộc tính;
  chưa đăng nhập được chuyển về HTTPS login; POST thiếu CSRF trả 403.
  Đăng nhập → bắt buộc đổi mật khẩu → đăng nhập lại → mở trang chủ/tồn kho/vốn/nhập/xuất
  → đăng xuất đều đạt. Chỉ thay mật khẩu tài khoản thử; không chạm tài khoản cửa hàng.
- Tiến trình thử đã dừng, CSDL tạm đã xóa; log và script thử chỉ nằm trong `.tools/`
  được Git bỏ qua. Không gửi dữ liệu cửa hàng hoặc bí mật lên bên ngoài.

Tệp mới/sửa trong đợt này: `Dockerfile`, `.dockerignore`, `render.yaml`, `.gitignore`,
`src/main/resources/application-prod.properties`,
`src/main/java/com/vatlieu/web/AccountController.java`, `TRIEN-KHAI-NEON-RENDER.md`,
`README.md`, `HUONG-DAN-DU-AN.md`, `AGENTS.md` và tài liệu này.

Chưa kiểm tra image Docker thực tế (máy chưa có Docker), kết nối SSL tới Neon,
khởi động/ngủ trên Render, tải đồng thời và mạng điện thoại thật. Chưa commit/push,
chưa xác nhận URL online, chưa chuyển dữ liệu hoặc thiết lập sao lưu tự động.
Màn hình Neon đang yêu cầu người dùng đăng nhập; lựa chọn dữ liệu trống/chuyển dữ liệu
chưa có trả lời. Không coi các bước cloud này là hoàn tất từ kết quả chạy local.

## Thao tác nhập, bán và công nợ · 18/11/2026

- Giao diện đã sửa trong `purchase-form.html`, `outbound-form.html`, `capital.html`,
  `fragments.html`, `sky.css`, `batch-select.js` và `settlement.js`.
  `OperationsController.java` trả về đúng tab công nợ sau khi ghi tiền.
- Đã cập nhật `PurchasePostgresTest.java`, `OperationsPostgresTest.java`, `DESIGN.md`,
  `UX-CONTRACT.md`, `AGENTS.md` và báo cáo này. Prettier, 55 test Java có PostgreSQL
  và 20 test JavaScript đều qua. Bản ứng dụng đã khởi động được.
- Chrome dùng CSDL tạm riêng. Nhập 6 kg với giá 12.000 VNĐ/kg, trả 10.000 VNĐ,
  mã lô tự điền có thể sửa. Bán 2 kg với giá 20.000 VNĐ/kg, thu 15.000 VNĐ.
  Bật rồi tắt Tất toán giữ tiền đã nhập. Thêm rồi xóa dòng vẫn giữ lô của dòng đầu.
  Thu thêm 5.000 VNĐ còn 20.000 VNĐ. Hủy hộp xác nhận thu, tất toán phiếu nhập
  62.000 VNĐ và kiểm tra phiếu rời danh sách cần trả.
- Đã thử gửi thiếu tên hàng, mở mục bằng bàn phím, màn hình hao hụt và kiểm tra
  chiều rộng 360, 390, 1080 px. Chưa thử trên điện thoại vật lý.
  Audit tĩnh vẫn báo bảy nút gắn sự kiện bằng JavaScript như lần trước.
- Trên web local, đăng nhập rồi chọn Nhập hàng, Xuất bán hoặc Vốn và công nợ → Công nợ.

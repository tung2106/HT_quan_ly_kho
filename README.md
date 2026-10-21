# Hệ thống quản lý vật liệu xây dựng & nông sản (Bước 2 — quản lý nội bộ)

Spring Boot 3 + PostgreSQL + Thymeleaf (giao diện ưu tiên điện thoại).

## Chức năng đã có
- Danh mục (đánh dấu hàng dễ hư), sản phẩm, quy đổi đơn vị (1 bao = 50 kg...)
- Nhà cung cấp
- Nhập hàng nhiều dòng: tự quy đổi về đơn vị gốc, phân bổ cước vận chuyển vào giá vốn,
  tạo lô, ghi giao dịch kho, cập nhật giá vốn bình quân gia quyền, ghi nhận công nợ NCC
- Màn hình tồn kho: số lượng, giá vốn, giá trị tồn, cảnh báo sắp hết
- Xuất bán nội bộ và hao hụt: quy đổi đơn vị, xuất FIFO theo ngày nhập rồi mã lô,
  chặn vượt tồn, lưu giá vốn thực tế, tính lại bình quân từ các lô còn tồn.
- Vốn / Nợ: doanh thu, giá vốn bán, hao hụt, vốn tồn, thu/trả và công nợ theo phiếu;
  thu/trả bổ sung nhiều lần và lưu lịch sử. Không có giao diện khách mua.

## Dùng chức năng mới
Ứng dụng yêu cầu đăng nhập. Quản lý tạo/khóa tài khoản và đặt lại mật khẩu tại
**Trang chủ → Quản lý tài khoản**; không có đăng ký công khai. Người dùng thường
chỉ xem tồn/giá bán, lập phiếu bán và xem phiếu do chính mình lập, không thấy giá vốn,
lợi nhuận hoặc tổng công nợ. Phiếu cũ chưa có người lập chỉ quản lý được xem.
Nhập hàng, hao hụt, công nợ, sửa sản phẩm, danh mục và NCC chỉ dành cho quản lý.
Tài khoản mới/được đặt lại mật khẩu phải đổi mật khẩu trước khi dùng. Khóa hoặc đổi
mật khẩu làm phiên đăng nhập cũ hết hiệu lực ở yêu cầu tiếp theo. Không cho tự khóa
tài khoản đang dùng hoặc khóa quản lý hoạt động cuối cùng.

Lần khởi động đầu tiên trên CSDL chưa có tài khoản, đặt biến môi trường
`APP_ADMIN_USERNAME` và `APP_ADMIN_PASSWORD` để tạo quản lý đầu tiên.
Mật khẩu ít nhất 12 ký tự, tối đa 72 byte UTF-8; lưu bằng BCrypt. Không ghi mật khẩu
vào mã nguồn hoặc Git. Khi đã có tài khoản, các biến này không đặt lại mật khẩu.
Sau khi tạo thành công, bỏ biến mật khẩu khởi tạo khỏi môi trường khởi động.

Quản lý có thể thêm/sửa/xóa danh mục; danh mục có sản phẩm (kể cả ngừng kinh doanh)
không được xóa. Nút **Quay lại** ở đầu màn hình chức năng trở về trang trước cùng
website, hoặc trang chủ nếu mở trang trực tiếp. Phiếu nhập mở sẵn một dòng hàng.

Trang `/` là trang chủ, có lối vào tất cả chức năng; thanh điều hướng dưới cùng có nút Trang chủ.
Trong phiếu nhập, xuất bán và hao hụt, chọn sản phẩm trước rồi chọn đơn vị từ danh sách
đơn vị gốc/quy đổi đã khai báo. Đổi sản phẩm sẽ tự chọn lại đơn vị gốc của sản phẩm mới.
Thêm quy đổi tại **Sản phẩm → Sửa → Quy đổi đơn vị** khi cần đơn vị chưa có.

1. Nhập hàng như trước. Tiền đã trả để 0 nếu còn nợ NCC.
2. Mở **Xuất kho → Xuất bán**, nhập sản phẩm, số lượng, đơn vị và giá bán thực tế
   theo đơn vị đó. Nếu chưa thu đủ, ghi tên/thông tin liên hệ khách.
3. Mở **Xuất kho → Hao hụt**, nhập số lượng và lý do. Kho giảm, không phát sinh tiền thu.
4. Mở **Vốn / Nợ** để ghi thu khách hoặc trả NCC theo từng phiếu và xem lịch sử.

Quy ước: số lượng sau quy đổi tối đa 3 số lẻ, đơn giá tối đa 2 số lẻ theo cấu trúc
CSDL hiện có. Cộng toàn bộ tiền hàng (và cước nhập) trước khi làm tròn tổng đến 1 đồng,
0,5 làm tròn lên. Khoản thu/trả mới là số nguyên đồng và không được vượt nợ.
Giá trị tồn lấy trực tiếp tổng số lượng còn lại × giá vốn từng lô, không nhân với
giá vốn bình quân đã làm tròn. Ngày xuất/thu/trả là thời điểm ghi nhận, chưa hỗ trợ ghi lùi ngày.

Migration V2 giữ phiếu nhập cũ, chốt tổng từ dữ liệu đang có và chuyển số đã trả
sang lịch sử thanh toán; ngày khoản cũ lấy theo ngày phiếu, không phải ngày trả được xác minh.
Chi tiết độ chính xác đã mất trong phiếu nhập cũ không thể khôi phục từ dữ liệu hiện có.
Lãi gộp sau hao hụt chưa trừ chi phí hoạt động; chênh lệch thu/trả chưa bao gồm tiền
đầu kỳ, vốn góp/rút, nên không phải lợi nhuận ròng hoặc số dư quỹ.

## Chạy thử
Yêu cầu: JDK 17+, Maven 3.9+, Docker.

    docker compose up -d        # chạy PostgreSQL
    mvn spring-boot:run         # tạo bảng tự động bằng Flyway (V1__init.sql)

Mở http://localhost:8080. Từ điện thoại cùng Wi-Fi: http://<IP máy tính>:8080

## Thứ tự dùng thử
1. Danh mục: thêm "Xi măng", "Sắn" (tick "Hàng dễ hư")
2. Sản phẩm: thêm "Xi măng PC40", đơn vị gốc `kg`, rồi vào sửa, thêm quy đổi `bao` = 50
3. Nhà cung cấp: thêm 1 NCC
4. Nhập hàng: chọn sản phẩm, số lượng 20, đơn vị `bao`, đơn giá 90000 -> kho sẽ ghi 1000 kg, giá vốn 1800/kg
5. Xem Tồn kho

## Kiểm thử
Chọn JDK 17 qua `JAVA_HOME` và đưa Maven vào `PATH` trước khi chạy.

    mvn clean package

Lệnh trên chạy unit test giá vốn và test render form nhập hàng, không cần PostgreSQL.
Test tích hợp PostgreSQL mặc định bỏ qua. Để kiểm tra Flyway, Hibernate `validate`,
các trang Thymeleaf và kịch bản 20 bao × 50 kg, chạy trong PowerShell:

    docker compose up -d
    docker compose exec -T db pg_isready -U vatlieu -d vatlieu
    $env:RUN_POSTGRES_TESTS = 'true'
    mvn test
    Remove-Item Env:RUN_POSTGRES_TESTS
    mvn spring-boot:run

Chỉ chạy test sau khi `pg_isready` báo nhận kết nối. Test tích hợp dùng PostgreSQL
theo `application.properties`, tạo dữ liệu qua controller và kiểm tra trực tiếp CSDL:
tồn 1000 kg, giá vốn sản phẩm/lô/giao dịch 1800/kg. Dữ liệu kịch bản được rollback
sau test; Flyway vẫn áp dụng migration khi khởi động context.

Test Bước 2 gồm: FIFO nhiều lô, số lượng lẻ/quy đổi, xuất hết/vượt tồn, làm tròn tổng,
thu/trả từng phần, gửi lại phiếu, rollback toàn phiếu lỗi, hai người cùng xuất/thu tiền,
render trang và nâng cấp phiếu nhập cũ từ V1 lên V2. Test đồng thời dùng dữ liệu có
mã riêng rồi dọn sau test; nên chạy trên CSDL kiểm thử riêng qua các biến môi trường
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.

## Chưa có (làm ở các bước sau)
- Triển khai Internet/HTTPS, chống thử mật khẩu hàng loạt và khôi phục tài khoản qua email.
- Giá sỉ theo bậc, sửa/hủy phiếu, trả hàng, kiểm kê điều chỉnh tăng/giảm,
  chi phí hoạt động, quỹ đầu kỳ/vốn góp/rút và báo cáo lợi nhuận theo kỳ.
- Phân tích tuổi tồn/vòng quay vốn, quản lý hồ sơ khách hàng riêng, lọc/phân trang lịch sử.

## Cấu trúc
    entity/      ánh xạ bảng
    repository/  truy vấn
    service/     logic nghiệp vụ (PurchaseService: giá vốn, StockService: tồn kho)
    web/         controller
    form/        đối tượng nhận dữ liệu từ form
    resources/templates/   giao diện Thymeleaf
    resources/db/migration/V1__init.sql   schema

## File thay đổi trong Bước 2
- Tạo: `src/main/resources/db/migration/V2__internal_operations.sql`.
- Tạo: `src/main/java/com/vatlieu/form/OutboundForm.java`;
  `service/InventoryMath.java`, `service/OperationsService.java`, `web/OperationsController.java`
  (các đường dẫn Java cùng gốc `src/main/java/com/vatlieu/`).
- Tạo giao diện trong `src/main/resources/templates/`: `outbound.html`, `outbound-form.html`,
  `outbound-detail.html`, `capital.html`.
- Tạo test: `src/test/java/com/vatlieu/OperationsPostgresTest.java`,
  `src/test/java/com/vatlieu/service/InventoryMathTest.java`.
- Sửa trong `src/main/java/com/vatlieu/`: `entity/PurchaseOrder.java`,
  `repository/ProductRepository.java`, `repository/BatchRepository.java`,
  `service/PurchaseService.java`, `service/StockService.java`, `web/PurchaseController.java`.
- Sửa: `src/main/resources/templates/fragments.html`, `src/main/resources/templates/stock.html`,
  `src/test/java/com/vatlieu/service/PurchaseServiceTest.java`, `.gitignore`, `AGENTS.md`, `README.md`.

Xác minh ngày 21/10/2026: 19 test đạt, 0 lỗi, 0 bỏ qua với Java 17 và PostgreSQL thử
riêng; Flyway V1 → V2 và Hibernate validate thành công. App khởi động trên cổng thử
18080; `/stock`, `/outbound`, `/outbound/new`, `/capital` trả HTTP 200.
Các công cụ portable trong `.tools/` và cache `.m2/` bị Git bỏ qua.

## File bổ sung cho phân quyền và điều hướng
- `pom.xml`: Spring Security và thư viện kiểm thử cùng phiên bản do Spring Boot quản lý.
- Migration mới `V3__accounts_and_ownership.sql`; giữ nguyên V1, V2.
- `config/SecurityConfig.java`; `service/AccountService.java`, `CurrentUser.java`,
  `CategoryService.java`; `web/AccountController.java`, `AccessModel.java`.
- Giao diện mới `login.html`, `password.html`, `users.html`, `forbidden.html`; script `static/js/back.js`.
- Cập nhật `OperationsService`, `PurchaseService`, `OperationsController`, `PurchaseController`,
  `CategoryController`; các trang home/products/stock/outbound/outbound-detail/categories/fragments.
- Bổ sung `AccessPostgresTest`, cập nhật test tích hợp và render để kiểm tra đăng nhập/CSRF.

Xác minh phần phân quyền ngày 21/10/2026: 26 test đạt, 0 lỗi, 0 bỏ qua; gồm truy cập
chéo phiếu, giả mạo người lập, chặn chức năng quản lý, khóa phiên, đổi mật khẩu lần đầu,
CSRF, sửa/xóa danh mục và chặn xóa danh mục có sản phẩm. App đã khởi động với V3.

# Hệ thống quản lý vật liệu xây dựng & nông sản (Bước 1)

Spring Boot 3 + PostgreSQL + Thymeleaf (giao diện ưu tiên điện thoại).

## Chức năng đã có
- Danh mục (đánh dấu hàng dễ hư), sản phẩm, quy đổi đơn vị (1 bao = 50 kg...)
- Nhà cung cấp
- Nhập hàng nhiều dòng: tự quy đổi về đơn vị gốc, phân bổ cước vận chuyển vào giá vốn,
  tạo lô, ghi giao dịch kho, cập nhật giá vốn bình quân gia quyền, ghi nhận công nợ NCC
- Màn hình tồn kho: số lượng, giá vốn, giá trị tồn, cảnh báo sắp hết

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

## Chưa có (làm ở các bước sau)
- Đăng nhập/phân quyền (CHƯA có, vì vậy KHÔNG mở ra internet ở giai đoạn này)
- Bán hàng, giá sỉ theo bậc, công nợ khách, hao hụt, báo cáo lợi nhuận

## Cấu trúc
    entity/      ánh xạ bảng
    repository/  truy vấn
    service/     logic nghiệp vụ (PurchaseService: giá vốn, StockService: tồn kho)
    web/         controller
    form/        đối tượng nhận dữ liệu từ form
    resources/templates/   giao diện Thymeleaf
    resources/db/migration/V1__init.sql   schema

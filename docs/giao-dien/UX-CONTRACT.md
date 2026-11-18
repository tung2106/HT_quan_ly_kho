# Quy ước tương tác của web quản lý kho

## Product context

5–6 người gia đình Việt Nam, nhập và tra cứu trên điện thoại. Tiếng Việt vi-VN,
múi giờ nghiệp vụ Asia/Ho_Chi_Minh (UTC+7), lịch Gregorian. Mục tiêu WCAG 2.2 AA.

## Business-context sources

| Phạm vi | Nguồn | Kiểm tra |
|---|---|---|
| Quyền OWNER/STAFF | AGENTS.md; config/SecurityConfig.java; service/CurrentUser.java | 09/11/2026 |
| Tiền, lô, nhập/xuất, tất toán | AGENTS.md; PurchaseService.java; OperationsService.java; InventoryMath.java | 09/11/2026 |
| Ngừng dùng và khôi phục | ProductService.java; SupplierService.java; AGENTS.md | 09/11/2026 |
| Định hướng và cách làm | [Hướng dẫn dự án](../HUONG-DAN-DU-AN.md); yêu cầu người dùng trong chat | 09/11/2026 |

Các đường dẫn Java ở `src/main/java/com/vatlieu/`. Quy tắc nghiệp vụ tham chiếu nguồn trên,
không định nghĩa lại trong tài liệu giao diện. Chưa có luồng pháp lý/thanh toán bên ngoài.

## Visual contract

Theo [thiết kế giao diện](DESIGN.md); token runtime là sky.css, không sinh bản CSS thứ hai.
Theme Sáng/Tối dùng cùng token, nhịp bố cục và các màu thu/trả. Mọi quyết định dùng chung
phải cập nhật CSS và DESIGN trong cùng thay đổi.

## Canonical UI Map

| Capability | Canonical owner | Source of truth | Allowed variants | Verification |
|---|---|---|---|---|
| Select/Listbox | native select/datalist + unit-select.js, batch-select.js, purchase-entry.js | AGENTS.md, DESIGN.md | native | chọn bằng bàn phím, gợi ý hàng cũ, nhóm/đơn vị |
| Date | native input date | form/controller hiện có | native | lọc hai mốc, giữ ngày nhập |
| Form | HTML constraints + ui.js + server validation | controller/service hiện có | create/edit/payment | test máy chủ, lỗi nhập, tiền vượt nợ |
| Scrollbar | sky.css | DESIGN.md | horizontal table | computed style, điện thoại |
| Toast | fragments alerts | controller flash attributes | success/error | role status/alert |
| CRUD | controller hiện có + ui.js confirmation | AGENTS.md và service | redirect/inline | AccessPostgresTest, OperationsPostgresTest |

Không có chọn hàng loạt. Popup select/ngày do hệ điều hành quyết định, chấp nhận hình học
và ngôn ngữ theo thiết lập thiết bị; không tự viết combobox/calendar mới trong đợt này.

## Flow ledger

| Thao tác | Đang chờ | Thành công | Lỗi/khôi phục | Nguồn |
|---|---|---|---|---|
| Nhập, xuất, sửa sản phẩm | chặn gửi trùng, hiện màn chờ sau 1,5 giây | về danh sách theo controller | giữ form theo controller, thông báo lỗi | Purchase/Operations/ProductController |
| Xóa/ngừng dùng | xác nhận tên và hệ quả, focus Hủy | danh sách, thông báo máy chủ | không tự ẩn trước khi server chấp nhận | Product/Supplier/CategoryController |
| Thu/trả, tất toán | xác nhận tất toán và khóa gửi trùng | về công nợ, lịch sử được lưu | server kiểm tra phần nợ hiện tại | OperationsController |
| Tìm NCC/lịch sử | GET khi bấm Tìm/Lọc | query trong URL | giữ bộ lọc, có xóa tìm kiếm | Supplier/OperationsController |
| Tìm công nợ | lọc tại chỗ | chỉ lọc tab hiện tại | trạng thái không tìm thấy | debt-tabs.js |
| Quay lại | GET tới trang trước | không gửi lại POST | dự phòng trang chủ | back.js |

## Navigation and responsive behavior

- Dùng một thanh chức năng có nhãn và aria-current theo nhóm. Dưới 900px thanh nằm ở cạnh dưới.
  Từ 900px thanh nằm trên đầu trang. Sản phẩm và lô thuộc Hàng còn, phiếu nhập thuộc Nhập hàng.
- Cài đặt dùng chung ở góc phải: Sáng/Tối, Sản phẩm và Quản lý tài khoản cho OWNER,
  Đổi mật khẩu và Đăng xuất cho người đã đăng nhập. STAFF không thấy công cụ quản lý;
  vẫn xem giá bán từ Hàng còn. Quyền máy chủ giữ nguyên.
- Trang chủ chia công việc thành Ghi giao dịch, Theo dõi hàng & tiền và Tra cứu phiếu.
  Quản lý thấy Nhập hàng, Xuất bán và Ghi hao hụt. Nhân viên thấy Xuất bán.
  Menu dùng dialog native: focus vào Đóng, Escape hoặc chạm nền bên ngoài để đóng,
  trả focus về nút Cài đặt; kéo từ bên trong ra ngoài không đóng nhầm,
  không gửi hoặc xóa dữ liệu form đang nhập khi mở/đóng hay đổi theme.
- Theme lưu theo trình duyệt bằng khóa vatlieu-theme, áp dụng cả trang đăng nhập và đồng bộ
  giữa tab cùng nguồn. Nếu lưu trữ bị chặn, vẫn đổi được trên trang hiện tại; mặc định Sáng.
- Vốn/nợ: nút Trang chủ và Danh mục vốn và công nợ cùng hàng; các mục mở màn hình riêng.
- Công nợ: tab Thu nợ khách hàng/Trả tiền nhập hàng, chỉ phiếu còn nợ; giữ hash và tìm kiếm riêng từng tab.
- Nhập hàng có tên người bán tùy chọn (150 ký tự), giữ nội dung khi lỗi. Tên dùng trên phiếu,
  công nợ, lịch sử trả và lô; phiếu cũ lấy tên NCC, phiếu không ghi tên vẫn phân biệt bằng mã phiếu.
- Nhập mới chỉ một mặt hàng; tên mới lưu cùng phiếu, lỗi thì rollback cả tên hàng mới.
  Mã lô cấp trước có thể sửa; số thứ tự được phép có khoảng trống khi mở form rồi không lưu.
  Trùng mã lô báo lỗi, giữ dữ liệu; gửi lại cùng request_key trả về phiếu đã lưu.
- Lịch sử nhập/xuất dùng tìm kiếm GET trong URL, chữ có dấu/không dấu tương đương khi tìm.
  Nhân viên vẫn chỉ tìm được phiếu bán do mình lập. Tên lưu không bị bỏ dấu hoặc gộp theo tìm kiếm.
  Mở Chi tiết phiếu nhập để xem số lượng, ghi chú và nút xử lý phiếu. Nút Thanh toán
  của phiếu còn nợ luôn hiển thị ngay trên danh sách.
- Quản lý có thể chuyển phiếu nhập vào Thùng rác sau khi xác nhận số phiếu.
  Chặn xóa nếu đã trả tiền, xuất bán hoặc ghi hao hụt từ lô. Khi xóa, ghi đối ứng từng lô,
  tính lại vốn và bỏ khoản phải trả. Khôi phục dùng lại phiếu, ngày nhập, mã lô và giá vốn gốc.
  Thùng rác có tìm kiếm, xem chi tiết và khôi phục.
- Ở lịch sử xuất, chỉ quản lý được chuyển phiếu bán chưa thu tiền vào Thùng rác sau khi xác nhận.
  Hàng trở về đúng các lô đã xuất, phiếu rời doanh thu, giá vốn đã bán và công nợ.
  Khôi phục chỉ thành công khi từng lô gốc còn đủ số lượng. Nhân viên chỉ thấy phiếu đang sử dụng của mình.
- Trang có một h1 và một vùng main; form cuộn tự nhiên, thanh dưới không che nút cuối.
- Bảng tồn giữ quan hệ các cột và cuộn ngang trong vùng có tên; không giấu giá trị.
- Hàng còn và Các lô còn tồn dùng cùng bộ lọc native Tất cả/Vật liệu xây dựng/Nông sản,
  bấm Lọc áp dụng GET và giữ tham số group khi chuyển từ bảng tồn sang xem lô.
  Mỗi sản phẩm một tiêu đề; các lần nhập nằm bên dưới, phân biệt bằng mã lô.
  Tồn không dương ưu tiên nhãn Hết hàng, còn hàng dưới mức tối thiểu hiện Sắp hết;
  bằng mức tối thiểu chưa cảnh báo. Không sửa dữ liệu tồn hoặc mức tối thiểu để bật cảnh báo.

## Forms, dialogs and feedback

- Form khai báo novalidate; `ui.js` cung cấp validation tiếng Việt tại ô, focus ô lỗi đầu tiên;
  không bỏ kiểm tra máy chủ. Dùng constraints và customValidity của money-input.js.
- Label liên kết input, cả các dòng nhân bản. Không lấy placeholder làm nhãn duy nhất.
- Tìm kiếm có nút xóa nội dung; GET vẫn cần bấm Tìm để áp dụng, công nợ lọc ngay.
- Xác nhận dùng dialog HTML dùng chung, focus Hủy, Esc đóng, trả focus về nút đã bấm.
  Tất toán không được gửi form chỉ vì mở hoặc đóng dialog.
- Thông báo lỗi giữ trên trang; thành công là status từ máy chủ, không tạo toast trùng.
- Phiếu xuất bán chọn thu đủ bằng nút Tất toán (`settlement.js`, `aria-pressed`);
  bấm lại bỏ chọn và giữ số tiền nhập trước đó. Nút không gửi phiếu; chỉ Lưu phiếu bán
  mới ghi giao dịch. Khi máy chủ trả lỗi, giữ trạng thái đã chọn. Phiếu nhập vẫn dùng ô tích.
- Trên phiếu nhập, nhập tên hàng trước. Ngày, mã lô và ghi chú nằm trong mục có thể mở,
  tự mở khi máy chủ trả lỗi. Phiếu bán hiển thị tên lô đã chọn ngay cả khi thu mục chọn lô.
- Mỗi phiếu còn nợ có mục Thu/Trả một phần và nút Tất toán. Khi ghi tiền xong hoặc có lỗi,
  ứng dụng trở lại đúng tab thu hoặc trả.
- Chỉ tự giãn textarea, không để kéo làm vỡ bố cục. Mật khẩu mặc định che,
  nút mắt riêng cho từng ô khi đăng nhập, đổi và cấp mật khẩu, cho phép dán và trình quản lý mật khẩu.

## Async and recovery

Form POST theo phản hồi máy chủ, không optimistic, không tự gửi lại khi mất mạng.
Chống gửi trùng client đi cùng UUID/khóa máy chủ hiện có; client không thay server.
Mèo hiện khi đã chờ 1,5 giây. Nếu tiếp tục chờ lâu, người dùng có thể ẩn màn hình chờ.
Không lưu mật khẩu hay form công nợ vào localStorage. Phiên hết hạn về đăng nhập.

## Migration status and remaining work

Đợt này chuẩn hóa shell, trang chủ, cách chọn mục vốn/nợ, style form/bảng và tương tác dùng chung.
Danh sách cũ vẫn tải toàn bộ từ server; cần thêm phân trang máy chủ cho lịch sử và dữ liệu lớn
trong một đợt có test truy vấn riêng. Không gọi cách hiện tại là đã tối ưu dữ liệu lớn.
Khôi phục bản nháp sau hết phiên, cảnh báo rời form chưa lưu và phân trang là phần chuyển đổi
còn lại, chưa được bảo đảm trong toàn bộ app. Không thêm quy tắc lưu PII ở trình duyệt tự phát.

## Verification

- `mvn test`, bật RUN_POSTGRES_TESTS với CSDL kiểm thử riêng; `node --test src/test/js/*.test.cjs`.
- Lint DESIGN bằng `npx -p @google/design.md designmd lint docs/giao-dien/DESIGN.md`.
- Audit skill: `py -3 <frontend-design-premium>/scripts/audit_project.py . --mode strict`.
- Kiểm tra điện thoại 360/390px và desktop, mở popup native, Tab/Escape, tên dài, tiền lớn,
  xóa tìm kiếm, dialog Hủy, quyền STAFF. Không gửi thử phiếu trên dữ liệu thật.
- Các giới hạn chưa kiểm tra phải báo rõ; static audit không thay kiểm tra trình duyệt.
- Kết quả và các nhận diện nhầm của audit tĩnh: xem mục cập nhật gần nhất trong
  [báo cáo kiểm thử](../kiem-thu/KIEM-TRA-CHAT-LUONG.md); không coi strict audit là đạt khi còn báo lỗi.

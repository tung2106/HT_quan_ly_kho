---
version: alpha
name: "Kho gia đình"
description: "Sổ kho dễ đọc trên điện thoại, sắc xanh bầu trời và mặt giấy trắng."
colors:
  primary: "#0868ac"
  ink: "#173b56"
  muted: "#526b7c"
  background: "#edf6fb"
  surface: "#ffffff"
  line: "#cbdde9"
  success: "#17643d"
  danger: "#a32c36"
typography:
  heading:
    fontFamily: '"Segoe UI", system-ui, sans-serif'
  body:
    fontFamily: '"Segoe UI", system-ui, sans-serif'
rounded:
  DEFAULT: "12px"
  control: "10px"
spacing:
  section-gap: "24px"
  page-max: "1080px"
components:
  button:
    backgroundColor: "{colors.primary}"
    rounded: "{rounded.control}"
    height: "46px"
  card:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.DEFAULT}"
  dialog:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
  table:
    textColor: "{colors.ink}"
  divider:
    backgroundColor: "{colors.line}"
  supporting-text:
    textColor: "{colors.muted}"
  receipt-text:
    textColor: "{colors.success}"
  payment-text:
    textColor: "{colors.danger}"
---

# Thiết kế giao diện Kho gia đình

## Overview

Giao diện như một cuốn sổ kho mở trên quầy: tên hàng và số tiền dễ đọc, thao tác rõ,
ít trang trí. Dành cho gia đình Việt Nam, điện thoại là thiết bị chính; ngôn ngữ vi-VN.
Đây là ứng dụng nội bộ, không phải trang quảng cáo hay cửa hàng cho khách mua.

Dấu hiệu nhận diện: nền xanh trời nhạt, mây trắng nhẹ ở ngoài vùng đọc;
mèo may mắn ở đầu trang chủ và trong màn hình chờ theo yêu cầu của người dùng.
Giữ mèo khi làm gọn giao diện, không thay bằng icon kho. Mèo trang chủ rộng 88–112px,
giữ tỉ lệ ảnh và đứng yên để các nút chức năng vẫn dễ thấy.
Hạn chế hình lớn, bóng đậm và thẻ quá cao.
Có giao diện Sáng/Tối trong Cài đặt; mặc định Sáng, nhớ lựa chọn trên trình duyệt.
Phông hệ thống có dấu tiếng Việt, không tải thêm phông ngoài.

Nguồn token chuẩn là `src/main/resources/static/css/sky.css` (model B);
tài liệu này ghi ý nghĩa và phản ánh giá trị. Đường đi: CSS variables → Bootstrap
button/form adapter và lớp dùng chung → template. Không thêm bảng màu riêng cho từng trang.

## Colors

`primary` cho điều hướng và hành động chính; `surface` là vùng đọc trắng;
`ink` và `muted` phân cấp chữ. `success` chỉ thu khách/thành công;
`danger` cho trả tiền nhập hàng, lỗi và thao tác xóa, luôn kèm chữ mô tả.
Màu thu xanh lá và trả đỏ giữ theo yêu cầu người dùng. Focus dùng viền primary rõ.
Theme tối dùng cùng token ở `:root[data-bs-theme='dark']` trong sky.css:
nền #0f1c29, mặt nội dung #182b3c, chữ #e0edf7, chữ phụ #aec0cf, viền #395165,
xanh chính #82c9f7, thu #84dbb0 và trả #ff9da6. Mây giảm độ sáng, không đảo màu ảnh mèo.
Bootstrap dùng cùng `data-bs-theme`; theme.js áp dụng trước khi vẽ trang.

## Typography

Heading dùng Segoe UI đậm 700, cỡ 24–32px; nội dung cùng họ chữ 16px, line-height 1.5.
Tách vai trò bằng cỡ/độ đậm thay vì thêm font gây tải chậm. Chú thích tối thiểu 13px.
Tiền canh phải khi so sánh, dùng tabular-nums, đủ dấu chấm và VNĐ.
Số lượng bỏ số 0 thập phân thừa, không nhóm hàng nghìn. Tên hàng dài được xuống dòng.

## Layout

Biểu mẫu giao dịch đặt mặt hàng trước phần tiền. Dùng `details/summary` cho thông tin
ít sửa và thao tác thu/trả một phần. Dòng mở cao tối thiểu 46px, dùng màu chữ và nền từ sky.css.

Các ô nhập đơn giá, tổng tiền lô và tiền đã thu/trả dùng chung khung `money-entry`
nền xanh, số tiền đậm và ô cao tối thiểu 52px. Cước vận chuyển dùng kiểu ô phụ.

Max-width 1080px, lề điện thoại 14px, nhịp nội dung 8/12/16/24px.
Trang chủ: tiêu đề và mèo → nhập/bán → ba số vốn/chi/thu → hàng còn và lô → vốn/nợ → lịch sử.
Cài đặt ở góc phải thanh đầu trang, chứa giao diện và công cụ quản lý/tài khoản.
Hao hụt vẫn ở luồng hàng hóa, không đưa giao dịch vào Cài đặt.
Hai cột thao tác trên điện thoại; cụm hàng hóa có thể ba cột từ 768px.
Khung Cài đặt dùng dialog native, mở bên phải, vừa màn hình nhỏ và cuộn nội dung khi cần.
Thanh điều hướng dưới giữ tối đa 5 mục, vùng chạm ≥44px và safe-area.
Form cuộn theo trang, không khóa chiều cao viewport. Bảng nhiều cột cuộn ngang trong
khung riêng, có hướng dẫn; không ép toàn bộ trang tràn ngang hoặc giấu số liệu.

## Elevation & Depth

Vùng nội dung dùng viền nhẹ; chỉ một bóng nhẹ cho thẻ và thanh điều hướng.
Không áp hover như nút lên thẻ chỉ đọc. Mây không nhận click và không che nội dung.
Dialog ở top layer, loading phủ trang nhưng có đường ẩn khi chờ quá lâu.

## Shapes

Thẻ bo 12px, nút và ô nhập 10px. Nút đủ cao 46px, padding đều.
Mỗi nhóm dùng một kiểu icon nét SVG ở điều hướng chính; icon chỉ bổ trợ nhãn chữ.

## Components

| Token tài liệu | CSS runtime | Thành phần dùng |
|---|---|---|
| colors.primary | --sky-blue | nút, focus, link |
| colors.ink / muted | --sky-ink / --sky-muted | chữ chính/phụ |
| colors.background / surface | --sky-bg / --sky-surface | body, card |
| colors.line | --sky-line | input, viền thẻ |
| colors.success / danger | --sky-success / --sky-danger | thu/trả, trạng thái |
| rounded.DEFAULT / control | --sky-radius / --sky-control-radius | card, control |
| spacing.page-max | --sky-page-max | container |

`fragments.html` sở hữu navigation, icon và thông báo. `ui.js` sở hữu xác nhận,
xóa nội dung tìm kiếm, mở/đóng dialog tiện ích và phản hồi nhập liệu.
`theme.js` sở hữu lựa chọn Sáng/Tối; chỉ lưu tên theme, không lưu dữ liệu tài khoản/giao dịch.
`loading.js` chỉ hiện mèo khi chuyển trang hoặc gửi biểu mẫu phải chờ từ 1,5 giây.
Trang mới hiển thị trực tiếp; chuyển động tuân theo cài đặt giảm chuyển động.

Mặc định/hover/pressed/focus/disabled đều có kiểu riêng. Nút chính đặc xanh; nút phụ
viền, xóa viền đỏ; dialog tập trung vào nút Hủy trước. Trạng thái đang gửi không
đổi kích thước nút. Nội dung không hiện thông báo thành công trước phản hồi máy chủ.
Select và date dùng control native để tận dụng bàn phím/chọn ngày trên điện thoại.
Tên hàng khi nhập dùng input + datalist native; máy chủ xác định hàng cũ/mới và chặn
trùng tên không phân biệt được. Không cần màn hình tạo sản phẩm trước khi nhập.
Tất toán trên phiếu xuất bán dùng nút rộng cả hàng, màu thu khách, cao tối thiểu 46px.
Trạng thái được chọn dùng nền xanh lá và dòng chữ riêng, không chỉ dựa vào màu.

## Do's and Don'ts

- Mỗi màn hình chỉ một tiêu đề chính, giữ tên chức năng nhất quán.
- Vốn/nợ là màn hình chọn mục; chỉ mở nội dung sau khi chọn, công nợ giữ hai tab riêng.
- Giữ link sản phẩm, lô tồn và bảng tồn; không tự đổi luồng kho thành màn hình danh mục.
- Nhập hàng dùng tên người bán tùy chọn, không có bước quản lý/chọn NCC ở luồng chính.
- Mỗi lần nhập một hàng, mã lô tự tạo có thể sửa; số lượng theo đơn vị gốc để ít thao tác.
- Không dùng splash/hero lớn làm người dùng phải cuộn mới thấy nhập/bán.
- Không đổi dữ liệu tài chính, làm tròn hay quyền để phù hợp bố cục.
- Kiểm tra 360/390px và desktop; dùng DOM/computed layout khi người dùng không muốn ảnh báo cáo.

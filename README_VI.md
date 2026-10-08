<div align="center">
<h1>LuckyTool</h1>
<img src="./assets/ic_launcher.png" alt="">
<p></p>
<p>
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README.md">简体中文</a> 丨
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README_EN.md">English</a> 丨
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README_ZH_TW.md">繁體中文</a> 丨
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README_JA.md">日本語</a> 丨
  <a href="https://github.com/luckyzyx/LuckyTool/blob/main/README_RU.md">Русский</a> 丨
  <b>Tiếng Việt</b>
</p>
<a href="https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool/releases"><img alt="GitHub all releases" src="https://img.shields.io/github/downloads/Xposed-Modules-Repo/com.luckyzyx.luckytool/total?label=Downloads"></a>
<a href="https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool/stargazers"><img alt="GitHub stars" src="https://img.shields.io/github/stars/Xposed-Modules-Repo/com.luckyzyx.luckytool"></a>
<a href="https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool/issues"><img alt="GitHub issues" src="https://img.shields.io/github/issues/Xposed-Modules-Repo/com.luckyzyx.luckytool"></a>
<a href="https://t.me/LuckyTool"><img alt="Telegram Channel" src="https://img.shields.io/badge/Telegram-Channel-blue.svg?logo=telegram"></a>
<a href="https://crowdin.com/project/luckytool"><img alt="Crowdin" src="https://badges.crowdin.net/luckytool/localized.svg"></a>
<p>Module Xposed mở rộng và tối ưu hoá cho ColorOS</p>
<p>Module miễn phí mãi mãi — mọi kênh thu phí đều không liên quan đến tác giả</p>
<p>Nghiêm cấm chuyển hướng truy cập, đăng lại, in lại, bán, chia sẻ hoặc tải lên lại khi chưa được phép</p>
<p>Hãy chia sẻ kiến thức thay vì chia sẻ thành quả đã hoàn thiện</p>
</div>

---

## Tổng quan

LuckyTool là một module Xposed chạy trên framework [LSPosed](https://github.com/LSPosed/LSPosed). Module hướng đến các thiết bị
  **ColorOS** và bổ sung hàng chục tính năng tăng cường cho framework hệ thống, SystemUI, trình khởi chạy, Settings và hơn 40 ứng dụng hệ thống. Các công tắc được đồng bộ tới các tiến trình hệ thống và có hiệu lực ngay lập tức — không cần khởi động lại.

Tài liệu hướng dẫn đầy đủ nằm tại [trang tài liệu](https://luckyzyx.gitlab.io/LuckyTool_Doc/).

## Tính năng

| Khu vực                                     | Điểm nổi bật                                                                                                                                                                                                                                      |
|---------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Framework hệ thống                          | số bậc âm lượng tuỳ chỉnh, ép buộc chia đôi màn hình, hỗ trợ ứng dụng 32-bit, danh sách trắng tối ưu hoá pin, giữ ứng dụng luôn chạy, bỏ qua kiểm tra ứng dụng rủi ro, cài đặt qua USB không cần xác nhận, và nhiều tính năng khác                |
| SystemUI / thanh trạng thái / màn hình khoá | kiểu đồng hồ và tốc độ mạng, ẩn hiện biểu tượng, các ô và độ trong suốt của Trung tâm điều khiển, kiểu thông báo, đồng hồ màn hình khoá và widget sạc, biểu tượng vân tay, và nhiều tính năng khác                                                |
| Trình khởi chạy                             | nền và hiệu ứng mờ của Dock, số hàng và số cột, giới hạn tên thư mục, số biểu tượng trên Dock, hành vi màn hình gần đây, xoá huy hiệu, và nhiều tính năng khác                                                                                    |
| Settings và ứng dụng hệ thống               | đổi DPI không cần khởi động lại, danh sách ứng dụng chế độ tối, ngôn ngữ riêng cho từng ứng dụng, vô hiệu hoá ứng dụng hệ thống, cùng các tăng cường cho Camera, Gallery, Game Assistant, Theme Store, Cloud Service và tổng cộng hơn 40 ứng dụng |
| Thời gian thực                              | thay đổi công tắc được đồng bộ tới các tiến trình hệ thống và áp dụng mà không cần khởi động lại                                                                                                                                                  |
| Đa phiên bản                                | các phần triển khai được tự động khớp với phiên bản ColorOS của bạn (12 – 16), luôn bám theo các bản phát hành mới                                                                                                                                |

Danh sách đầy đủ nằm trên trang tài liệu ([danh sách tính năng](https://luckyzyx.gitlab.io/LuckyTool_Doc/guide/features)); các công tắc bên trong ứng dụng là nguồn thông tin chính xác nhất.

## Yêu cầu

| Mục                    | Yêu cầu                                              |
|------------------------|------------------------------------------------------|
| Hệ thống               | ColorOS 12 trở lên (OPPO / OnePlus / realme)         |
| Phiên bản đã thích ứng | ColorOS 12 – 17, luôn bám theo các bản phát hành mới |
| Framework              | LSPosed                                              |
| Quyền                  | Một số tính năng yêu cầu Root                        |
| ABI thiết bị           | arm64-v8a                                            |
| Thiết bị đã kiểm thử   | OnePlus 15                                           |

## Tải xuống

Tải
  `LuckyTool_v*.apk` mới nhất từ [Releases](https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool/releases) và cài đặt như bình thường.

Tên tệp có dạng `LuckyTool_v<versionName>(<buildNumber>)_<buildType>.apk`. Con số trong ngoặc đơn là bộ đếm bản dựng giúp xác định chính xác bản build, vì vậy hãy ghi kèm nó khi báo lỗi.

## Cài đặt và kích hoạt

1. Tải và cài đặt APK của module.
2. Mở trình quản lý LSPosed → **Modules** → bật LuckyTool.
3. Trong **Scope**, tích chọn các ứng dụng hệ thống cần hook. Nên chọn tất cả: module sẽ tự bỏ qua phần logic không áp dụng cho từng gói.
4. Khởi động lại thiết bị (hoặc ít nhất là khởi động lại các scope bị ảnh hưởng).
5. Mở LuckyTool và bật công tắc tổng ở góc trên bên phải màn hình chính.

## Xử lý sự cố và câu hỏi thường gặp

- **Sau khi cập nhật hệ thống thì không có gì hoạt động**: dùng **Re-optimize Dex** trong menu khởi động lại của module, hoặc nhấn giữ ứng dụng trong scope ở LSPosed
  rồi tối ưu hoá lại nó. Ví dụ, nếu không xoá được thông báo tuỳ chọn nhà phát triển, hãy tối ưu hoá lại System UI.
- **Module hoàn toàn không hoạt động**: hãy chắc chắn rằng nó đã được bật trong LSPosed, ứng dụng mục tiêu nằm trong scope, không còn tệp `/sdcard/disable_lt` sót lại,
  và log của LSPosed cho thấy LuckyTool được nạp vào tiến trình chủ.
- **Một số công tắc không có tác dụng**: các tính năng được phân nhánh theo từng phiên bản ColorOS, nên sau một bản nâng cấp hệ thống lớn bạn phải chờ module thích ứng. Công tắc không hiển thị trong ứng dụng nghĩa là phiên bản của bạn chưa được hỗ trợ.
- Các câu hỏi khác được giải đáp trong [FAQ](https://luckyzyx.gitlab.io/LuckyTool_Doc/guide/faq);
  muốn phản hồi hãy dùng [Telegram](https://t.me/LuckyTool).

## Liên kết

| Mục                                          | Liên kết                                                      |
|----------------------------------------------|---------------------------------------------------------------|
| Tài liệu                                     | https://luckyzyx.gitlab.io/LuckyTool_Doc/                     |
| Nhật ký thay đổi                             | https://luckyzyx.gitlab.io/LuckyTool_Doc/changelog            |
| Kho module (LSPosed Repo, các bản phát hành) | https://github.com/Xposed-Modules-Repo/com.luckyzyx.luckytool |
| Phản hồi                                     | https://t.me/LuckyTool                                        |
| API Xposed                                   | [YukiHookAPI](https://github.com/HighCapable/YukiHookAPI)     |
| Giấy phép                                    | [GPL-3.0](./LICENSE.txt)                                      |

## Miễn trừ trách nhiệm

- Kho chính thức là [luckyzyx/LuckyTool](https://github.com/luckyzyx/LuckyTool)
  và lưu giữ đầy đủ lịch sử commit; bất cứ thứ gì được phát hành dưới tên dự án này ở nơi khác đều không liên quan đến tác giả.
- Tác giả có công việc chính và không sống bằng module này. Nó được mở mã nguồn theo GPL-3.0 như một điểm khởi đầu cho việc phát triển module.

## Lịch sử Star

<a href="https://www.star-history.com/#luckyzyx/LuckyTool&Timeline">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=luckyzyx/LuckyTool&type=Timeline&theme=dark" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=luckyzyx/LuckyTool&type=Timeline" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=luckyzyx/LuckyTool&type=Timeline" />
 </picture>
</a>

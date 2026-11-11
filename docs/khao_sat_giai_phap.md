# Khảo sát giải pháp liên quan (PLAN 9.4 — Hoàng)

> Tổng hợp từ mô tả công khai của các ứng dụng. Trước khi đưa vào báo cáo, nhóm nên tự cài thử (ít nhất Lookout và Seeing AI) để xác nhận lại tính năng ở thời điểm viết, và ghi ngày truy cập.

## Bảng so sánh

| Ứng dụng | Nền tảng | Đọc chữ | Tìm / nhận diện vật | Cách nhập lệnh | Kiểu phản hồi | Cần mạng | Tiếng Việt |
|---|---|---|---|---|---|---|---|
| **Seeing AI** (Microsoft) | iOS, Android | Đọc tức thì + chụp tài liệu, có hướng dẫn căn mép giấy | Nhận diện cảnh, người, tiền, mã vạch | Chạm, chọn "kênh" | Giọng nói liên tục, âm báo | Một phần chạy trên máy, mô tả cảnh cần mạng | Hạn chế |
| **Lookout** (Google) | Android | Chế độ Văn bản, Tài liệu | Chế độ Khám phá (đồ vật), Tìm (một số vật), tiền, nhãn thực phẩm | Chạm, TalkBack | Chỉ nói khi có vật / chữ mới | Phần lớn trên máy | Có hỗ trợ một phần |
| **Envision AI** | iOS, Android (+ kính) | Đọc tức thì, tài liệu nhiều trang | Mô tả cảnh, tìm vật, tìm người quen | Chạm | Giọng nói | Một phần | Hạn chế |
| **Be My Eyes** | iOS, Android | Qua tình nguyện viên hoặc trợ lý AI | Qua tình nguyện viên / AI | Gọi video | Người thật nói chuyện | Bắt buộc | Có (tình nguyện viên) |
| **VisionAid** (Nhóm 2, nền của VIAccess) | Android | OCR liên tục, đọc mọi chữ ổn định | Explore đọc mọi vật, Find theo nhóm vật, tiền | Giọng nói + vuốt | Đọc liên tục, rung theo khung vật | Không (trừ nhận diện tiền) | Có |

## Điều rút ra cho VIAccess
- **Ít lời hơn là tốt hơn**: Lookout chỉ nói khi có thay đổi; VisionAid đọc liên tục dễ gây quá tải (đúng nhận xét của cô). → Bộ điều phối phản hồi: một hàng đợi, ưu tiên, nghỉ 3 s giữa các lần lặp, tối đa 12 lời nhắc / phút.
- **Hướng dẫn căn camera**: Seeing AI có nhắc căn mép tài liệu trước khi chụp. → Luật căn camera 4.2, mỗi lần một lời nhắc, tự chụp khi ổn định.
- **Tìm một vật cụ thể**: chỉ một số app có, thường dùng âm thanh. → VIAccess dùng rung theo khoảng cách + nói hướng, để tai còn nghe môi trường.
- **Chạy trên máy, không cần mạng** cho nhận diện: an toàn dữ liệu, dùng được khi mất mạng. Riêng nhận lệnh giọng nói có thể cần mạng → luôn có nút chạm thay thế.
- **Không cạnh tranh** với Be My Eyes (người thật): VIAccess hướng tới việc nhanh, tự làm được.

## Nguồn (ghi ngày truy cập khi đưa vào báo cáo)
- Trang giới thiệu Seeing AI (Microsoft), Lookout (Google Play / Google Accessibility), Envision, Be My Eyes.
- Repo VisionAid (Nhóm 2, INT2041 2025–2026).

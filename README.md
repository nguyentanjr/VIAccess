# VIAccess

Ứng dụng Android giúp người khiếm thị **đọc văn bản** và **tìm đồ vật quen thuộc** bằng camera điện thoại, phản hồi bằng giọng nói và rung, không cần nhìn màn hình.

Đồ án môn Tương tác người máy (INT2041), Nhóm 13, Trường Đại học Công nghệ – ĐHQGHN.
Kế thừa từ repo VisionAid (Nhóm 2, INT2041 2025–2026); phần nhóm tự phát triển xem `PLAN.md` mục 1.4.

## Tính năng
- **Đọc văn bản:** app nhắc căn camera (ánh sáng yếu, giữ yên máy, ảnh mờ, đưa máy ra xa, lệch trái / phải, lại gần). Khi chữ rõ và ổn định, máy rung ngắn rồi tự đọc.
- **Tìm vật:** chọn một trong 6 vật (cốc, chai, điện thoại, điều khiển, sách, ghế). Máy rung nhanh dần khi lại gần và nói hướng trái / giữa / phải.
- **Lệnh giọng nói:** "đọc chữ", "tìm" + tên vật, "dừng", "tiếp", "đọc lại", "quay lại". Mỗi lệnh đều có nút chạm tương ứng ở cuối màn hình.
- Mọi xử lý nhận diện chạy trên máy, không gửi ảnh đi đâu.

## Cài đặt
Yêu cầu: Android 7.0 (API 24) trở lên; để build cần Android Studio (JDK 21 đi kèm).

```bash
git clone https://github.com/nguyentanjr/VIAccess.git
cd VIAccess
./gradlew assembleDebug        # tạo app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug         # cài lên máy đang cắm USB (bật gỡ lỗi USB)
./gradlew test                 # chạy unit test
```

Trên máy cần:
- Cho phép **camera** và **micro** khi app hỏi.
- Có **giọng đọc tiếng Việt** trong Cài đặt → Chuyển văn bản thành giọng nói (Google TTS → tải "Tiếng Việt"). Nếu thiếu, app sẽ hiện hộp thoại hướng dẫn cài.
- Nhận lệnh giọng nói dùng dịch vụ nhận dạng của máy; có thể cần mạng. Khi mất mạng, app báo và bạn dùng nút chạm.

## Sử dụng
1. Lần đầu mở app: làm theo hướng dẫn bằng giọng nói và cấp quyền.
2. Nói **"đọc chữ"** (hoặc chạm nút Đọc chữ), đưa camera về phía văn bản và làm theo lời nhắc. Nói **"dừng"** / **"tiếp"** / **"đọc lại"** khi đang nghe.
3. Nói **"tìm cốc"** (hoặc chạm nút Tìm rồi chọn vật), xoay máy chậm; nghe "Đã thấy cốc, bên trái" và đi theo rung.
4. Nói **"quay lại"** hoặc bấm nút Back để về màn chính; **"giúp đỡ"** để nghe danh sách lệnh.

## Cấu trúc chính
- `utils/FeedbackCoordinator.kt` – điều phối lời nói / rung chống quá tải
- `utils/FeedbackConfig.kt` – toàn bộ ngưỡng có thể hiệu chỉnh
- `utils/ImageQuality.kt`, `utils/MotionMonitor.kt`, `utils/AlignmentAdvisor.kt` – luật căn camera cho OCR
- `ui/screens/OCRScreen.kt`, `ui/screens/FindScreen.kt`, `ui/screens/MainScreen.kt`

Spec, phân công và tiến độ: `PLAN.md`.

# VIAccess — Nhóm 13, INT2041 (Tương tác người máy)

App Android hỗ trợ người khiếm thị. Kế thừa từ repo VisionAid (Nhóm 2); phải ghi nguồn trong báo cáo.
Spec, quy tắc phản hồi, chỉ số đánh giá và danh sách việc: xem `PLAN.md`.

## Phạm vi
- **Tác vụ 1 – Đọc văn bản:** hỗ trợ căn camera → tự chụp → OCR → đọc TTS; dừng / tiếp / đọc lại.
- **Tác vụ 2 – Tìm vật thể:** chọn 1 vật (cốc, chai, điện thoại, điều khiển, sách, ghế) → phản hồi rung + giọng nói.
- Hỗ trợ: ~5 lệnh giọng nói, TTS tiếng Việt, rung, giao diện chữ to/tương phản cao/TalkBack.
- **Ngoài phạm vi (không thêm):** Explore tự đọc mọi vật, nhận diện tiền, History, Themes, mô tả cảnh, dẫn đường, đọc văn bản nhiều cột, huấn luyện mô hình mới.

## Stack
Kotlin, Jetpack Compose, CameraX, TFLite YOLOv5s fp16 (`app/src/main/assets/yolov5s-fp16.tflite`, 80 lớp COCO), ML Kit Text Recognition, Android TTS (vi-VN), SpeechRecognizer, Vibrator. minSdk 24, targetSdk 34.
Package hiện tại: `org.tensorflow.lite.examples.shravan` (giữ nguyên trừ khi được yêu cầu đổi).

## Thành phần chính
- `tflite/CompositeAnalyzer.kt` – phát hiện tối + chuyển frame cho analyzer của màn hình
- `tflite/YoloAnalyzer.kt` – nhận diện vật thể, persistence, debounce
- `utils/OCRManager.kt` – kiểm tra ổn định/chống đọc lặp (Levenshtein)
- `utils/TTSManager.kt`, `utils/VoiceCommandManager.kt`, `utils/HapticManager.kt`, `utils/SettingsManager.kt`
- `ui/screens/OCRScreen.kt`, `ui/screens/FindScreen.kt`, `ui/screens/MainScreen.kt`

## Quy ước
- Mọi xử lý chạy trên thiết bị. Không gọi API mạng, không dùng API key.
- Chuỗi hiển thị/đọc để trong `res/values/strings.xml` (+ `values-vi`), không viết cứng.
- Ngưỡng và thời gian (confidence, số frame, khoảng nghỉ lời nhắc…) gom trong một object config (`utils/FeedbackConfig.kt`), không rải rác trong code.
- Phản hồi phải tuân theo quy tắc chống quá tải trong `PLAN.md` (mỗi lần 1 lời nhắc, có khoảng nghỉ, không nói và rung dày cùng lúc).
- Ghi log có timestamp (tag `VIA_METRIC`) cho các sự kiện cần đo: frame → phát hiện → rung/TTS.
- Quy ước Compose/tài nguyên chi tiết: `.agents/rules/*.mdc`.
- Xong một việc thì tick checkbox tương ứng trong `PLAN.md`.

## Quy trình làm việc nhóm (4 người, mỗi người một phiên Claude)
- Trước khi làm, hỏi người dùng là ai (Tân / Tiến / Hùng / Hoàng) nếu chưa rõ, rồi đọc mục của người đó trong `PLAN.md` mục 9.
- Mỗi người một nhánh riêng: `tan`, `tien`, `hung`, `hoang`. Chỉ commit vào nhánh của mình, không commit thẳng vào `main`. Trước khi làm: `git checkout <nhánh>` rồi `git pull` và `git merge origin/main` để lấy code mới của nhóm. Commit nhỏ, build được rồi mới push. Không dùng `--force`.
- Chỉ sửa file thuộc việc của mình (xem bảng file ở `PLAN.md` mục 5). Nếu cần sửa file dùng chung (`FeedbackCoordinator`, `FeedbackConfig`, `MainScreen`, `HapticManager`, `strings.xml`), sửa tối thiểu và ghi rõ trong commit message.
- Mọi lời nói và rung đi qua `FeedbackCoordinator`. Không gọi `ttsManager.speak` hay `vibrator` trực tiếp từ màn hình mới.
- Mọi ngưỡng mới thêm vào `FeedbackConfig`.
- Xong việc: build thành công → tick checkbox trong `PLAN.md` → thêm 1 dòng vào mục 12 Nhật ký → commit + push nhánh của mình → mở Pull Request vào `main`, một người khác xem rồi merge.
- Đổi quyết định hay gặp vấn đề ảnh hưởng người khác: ghi vào `PLAN.md` mục 11 và báo nhóm. Không tự mở rộng phạm vi.
- Claude không cầm được máy: sau khi sửa, nêu rõ người dùng cần thử gì trên máy thật (câu nói nào, rung thế nào thì là đúng).

## Lệnh
```
./gradlew assembleDebug
./gradlew test
./gradlew installDebug
```

# Chương: Kiến trúc và luồng tương tác (Tân)

> Bản nháp. Cần chèn ảnh sơ đồ (vẽ lại từ sơ đồ chữ bên dưới) và ảnh chụp màn hình.

## 1. Tổng quan
VIAccess là ứng dụng Android viết bằng Kotlin + Jetpack Compose. Toàn bộ nhận diện chạy trên máy:
- **Camera:** CameraX
- **Nhận diện vật:** YOLOv5s TFLite, 80 lớp COCO
- **Đọc chữ:** ML Kit Text Recognition
- **Giọng nói:** Android TextToSpeech (vi-VN) để đọc, SpeechRecognizer để nghe lệnh
- **Rung:** Vibrator

Ứng dụng không gọi API mạng nào của nhóm. Riêng dịch vụ nhận dạng giọng nói của hệ điều hành có thể cần mạng; khi đó luôn có nút chạm thay thế.

## 2. Sơ đồ khối
```
             ┌────────────── MainActivity ──────────────┐
             │ quyền, vòng đời (nền / tiếp tục), báo lỗi │
             └──────────────────┬───────────────────────┘
                                │
                         MainScreen (3 trang + thanh 5 nút)
             ┌──────────────────┼────────────────────┐
             │                  │                    │
   VoiceCommandManager   CameraX + CompositeAnalyzer  SettingsScreen
   (SpeechRecognizer)     (độ sáng → bật flash)
             │                  │
   VoiceCommandParser     ┌─────┴──────────┐
   (5 lệnh, đếm lỗi)      │                │
             │       YoloAnalyzer       OCRScreen analyzer
             │            │             ImageQuality, MotionMonitor
             │      FindStateMachine     AlignmentAdvisor, OCRManager
             │            │                │
             │            │          ReadingController (đọc theo câu)
             └────────────┴────────┬───────┘
                                   ▼
                        FeedbackCoordinator
          (hàng đợi, ưu tiên, cooldown, giới hạn / phút, tạm ngưng khi chạy nền)
                         ┌─────────┴─────────┐
                     TTSManager         HapticManager
                         │                   │
                     loa / tai nghe        rung
                                   │
                         MetricLogger (VIA_METRIC → CSV)
```

## 3. Thành phần chính
| Thành phần | Vai trò | Nguồn |
|---|---|---|
| `FeedbackCoordinator` | Điểm duy nhất phát lời nói và rung. Có 4 mức ưu tiên; lời mới chỉ ngắt lời cũ khi quan trọng hơn. Cùng một nội dung nghỉ 3 s; tối đa 12 lời nhắc / phút. Hỗ trợ dừng / tiếp / đọc lại; im lặng khi app chạy nền | Nhóm tự làm |
| `VoiceCommandParser` + `VoiceFailureCounter` | Nhận 5 lệnh theo từ nguyên vẹn sau khi bỏ dấu. Không hiểu 3 lần thì nhắc dùng nút chạm | Nhóm tự làm |
| `VoiceCommandManager` | Bọc SpeechRecognizer. Bổ sung: báo lỗi mạng / quyền, tự nghe lại, tạm ngưng khi app chạy nền | Kế thừa, sửa |
| `AlignmentAdvisor`, `ImageQuality`, `MotionMonitor` | Luật căn camera 4.2: độ sáng, Laplacian, gyroscope, vị trí và kích thước khung chữ | Nhóm tự làm |
| `ReadingController` | Tách câu, dừng / tiếp / đọc lại / câu trước / câu sau | Nhóm tự làm |
| `FindStateMachine`, `FindTarget` | Máy 5 trạng thái SEARCHING → FOUND → TRACKING → CENTERED → LOST; 6 vật mục tiêu | Nhóm tự làm |
| `YoloAnalyzer` | Chạy YOLO mỗi 5 frame, persistence 5 frame | Kế thừa, sửa |
| `OCRManager` | Ổn định 3/5 frame, chống đọc lặp (Levenshtein) | Kế thừa |
| `MetricLogger` | Ghi sự kiện có mốc thời gian, xuất CSV | Nhóm tự làm |
| `FeedbackConfig` | Gom toàn bộ ngưỡng để hiệu chỉnh | Nhóm tự làm |

## 4. Bảng kế thừa / tự phát triển (PLAN 1.4)
| Kế thừa từ VisionAid / thư viện | Nhóm tự phát triển hoặc điều chỉnh |
|---|---|
| CameraX pipeline, YOLOv5s TFLite, ML Kit OCR | Bộ điều phối phản hồi (ưu tiên, khoảng nghỉ, phân kênh, tạm ngưng khi chạy nền) |
| TTS, SpeechRecognizer, Vibrator wrapper | Phát hiện mờ (Laplacian), rung tay (gyroscope), vị trí khung chữ, luật căn camera |
| Auto-flash theo độ sáng, OCR ổn định 3/5 frame | Luật xác nhận 3 frame + conf ≥ 0.5; hướng trái / giữa / phải; mất dấu |
| Persistence 5 frame, debounce | Rung theo khoảng cách (250–1000 ms) |
| | 5 lệnh giọng nói + nút chạm; báo lỗi bằng giọng nói |
| | Đọc theo câu: dừng / tiếp / đọc lại |
| | Log đo chỉ số, bộ dữ liệu test, thử nghiệm A/B |
| **Đã bỏ:** Explore, nhận diện tiền (Roboflow, cần API key), Lịch sử, Giao diện, tiếng Anh | |

## 5. Luồng tương tác
```
Mở app ─► (lần đầu) hỏi mức thị lực ─► hướng dẫn + xin quyền
       └► màn Tìm: "Chế độ tìm kiếm. Bạn đang tìm gì?"
            ├─ "đọc chữ" / nút Đọc chữ ─► T1
            │     nhắc căn camera (1 điều / lần) ─► ổn định ─► rung ngắn ─► đọc từng câu
            │     "dừng" / "tiếp" / "đọc lại" (hoặc nút) · "quay lại" ─► màn Tìm
            ├─ "tìm cốc" / chọn nút Cốc ─► T2
            │     SEARCHING: rung nhẹ mỗi 2 s
            │     FOUND (3 frame): "Đã thấy cốc, bên trái" + rung dài
            │     TRACKING: rung theo khoảng cách; nói hướng khi đổi vùng (≥ 2 s)
            │     CENTERED (≥ 1 s): "Ở phía trước"
            │     LOST (> 1,5 s): "Mất dấu" + rung đôi ─► SEARCHING
            ├─ "giúp đỡ" ─► đọc màn hiện tại + danh sách lệnh
            └─ không hiểu ─► "Không hiểu, hãy nói lại" (lần 3: nhắc dùng nút chạm)
Lỗi bất kỳ lúc nào (ALERT, ngắt mọi lời khác): thiếu quyền, thiếu giọng tiếng Việt, mất mạng, lỗi mô hình.
App chạy nền: im lặng, không rung, ngừng nghe mic; camera dừng theo vòng đời.
```

## 6. Quy tắc chống quá tải — cài đặt cụ thể
- **Ưu tiên:** ALERT > STATE > GUIDANCE > INFO. Lời mới cao hơn lời đang đọc thì ngắt; thấp hơn hoặc bằng thì bỏ. Ngoại lệ: lời do người dùng yêu cầu (chạm, ra lệnh) luôn được đọc.
- **Cooldown và giới hạn:** dùng `key` của từng loại lời nhắc, ví dụ `align:OFF_LEFT` hay `error:network`.
- **Rung và nói:** không rung khi đang nói, trừ rung xác nhận. Không nghe lệnh khi đang nói (khoảng đệm 500 ms), trừ "dừng" và "đọc lại".

## 7. Kiểm thử
- **Unit test:** `[n]` test cho bộ điều phối, bộ phân tích lệnh, máy trạng thái Find, luật căn camera, bộ đọc theo câu. Chạy bằng `./gradlew test`.
- **Kiểm thử chức năng:** theo `docs/checklist_kiem_thu.md`. Kết quả: `[…]` đạt / `[…]` mục.

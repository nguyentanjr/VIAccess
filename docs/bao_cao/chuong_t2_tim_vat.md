# Chương: Tác vụ T2 – Tìm vật thể (Hùng)

> Bản nháp. `[…]` = điền sau khi đo. Không dùng số liệu của Nhóm 2.

## 1. Mục tiêu
Người dùng chọn 1 trong 6 vật: cốc, chai, điện thoại, điều khiển, sách, ghế. Có thể nói "tìm cốc" hoặc chạm nút. Ứng dụng hướng dẫn bằng nhịp rung theo khoảng cách và lời nói ngắn: đã thấy, hướng, ở phía trước, mất dấu.

## 2. Máy trạng thái
| Trạng thái | Điều kiện vào | Phản hồi |
|---|---|---|
| SEARCHING | Bắt đầu, hoặc sau LOST | Rung rất nhẹ mỗi 2 s, không nói |
| FOUND | Đúng lớp, ≥ 3 frame liên tiếp, conf ≥ 0.5 | "Đã thấy <vật>, <hướng>" rồi rung dài 300 ms |
| TRACKING | Sau FOUND | Rung lặp, chu kỳ = 1000 − 750 × (diện tích khung / diện tích ảnh), kẹp trong [250, 1000] ms. Chỉ nói hướng khi đổi vùng, cách nhau ≥ 2 s |
| CENTERED | Tâm khung ở vùng giữa ≥ 1 s | "Ở phía trước" (một lần) |
| LOST | > 1,5 s không thấy | "Mất dấu" rồi rung đôi → SEARCHING |

- **Vùng hướng:** x < 0.35 là trái, 0.35–0.65 là giữa, > 0.65 là phải.
- **Thấy lại sau khoảng trống:** nếu có khoảng trống không có frame nào (ví dụ camera tạm dừng) rồi lại thấy vật, máy xác nhận lại từ đầu chứ không báo "mất dấu".

## 3. Chế độ so sánh (baseline)
Không có luật 3 frame và không có trạng thái: hễ thấy vật trong frame là rung theo khung vật và đọc tên vật. Cách này giống VisionAid.

## 4. Đánh giá kỹ thuật
**Nhận diện:** 150 ảnh tự chụp (6 lớp × ~25 ảnh, 2 mức sáng), kèm ảnh không có vật để đo báo sai. Kết quả từ `python eval/eval.py detection`:

| Lớp | P | R | F1 |
|---|---|---|---|
| cốc | 0.92 | 0.96 | 0.94 |
| chai | 0.89 | 1.00 | 0.94 |
| điện thoại | 0.53 | 1.00 | 0.69 |
| điều khiển | 0.44 | 0.28 | 0.34 |
| sách | 0.00 | 0.00 | 0.00 |
| ghế | 1.00 | 0.12 | 0.21 |
| macro | 0.63 | 0.56 | 0.52 |

**Báo sai:** 20 lượt tìm có vật gây nhiễu, so sánh có / không có luật 3 frame. Số lần báo sai mỗi lượt: có luật `16`, không luật `34`, giảm `52.9%`.

**Độ trễ** (từ log `VIA_METRIC`, `python eval/eval.py latency`):
- Suy luận YOLO: `133.22` ms
- Từ frame đến lúc phản hồi: `137.63` ms
- Máy test: `[…]`

## 5. Hạn chế
- Chai nhựa trong suốt và điều khiển nhỏ khó nhận diện `[xác nhận bằng số liệu]`.
- Diện tích khung vật chỉ phản ánh khoảng cách một cách tương đối (vật to ở xa có thể giống vật nhỏ ở gần).
- Không tìm được vật ngoài COCO như chìa khóa, ví.
- Ví dụ quan sát trên máy ảo (2026-11-11): chuột gaming cầm trên tay bị YOLOv5s nhận là "điều khiển từ xa" (tới 0.79), không lần nào ra "chuột" trong 409 lượt — ảnh "mouse" của COCO chủ yếu là chuột nằm trên bàn, chụp từ trên xuống. Nhóm giữ nguyên (không gộp chuột với điều khiển) để tránh báo nhầm; cách khắc phục là huấn luyện thêm, ngoài phạm vi.
- Gỡ lỗi: `adb shell setprop log.tag.VIA_RAW DEBUG` rồi xem `adb logcat -s VIA_RAW` để thấy 3 kết quả thô cao nhất mỗi lượt.

# Chương: Tác vụ T1 – Đọc văn bản (Tiến)

> Bản nháp. `[…]` = điền sau khi đo trên máy thật. Không dùng số liệu của Nhóm 2.

## 1. Mục tiêu
Người dùng đưa camera về phía văn bản (hộp thuốc, hóa đơn, trang sách). Ứng dụng hướng dẫn căn camera cho tới khi đủ điều kiện, tự chụp rồi đọc theo từng câu. Người dùng không cần nhìn màn hình hay bấm nút chụp.

## 2. Luật căn camera
Mỗi frame được xét lần lượt; chỉ báo điều **đầu tiên** không đạt.

| Ưu tiên | Điều kiện | Ngưỡng | Lời nhắc |
|---|---|---|---|
| 1 | Thiếu sáng, flash bị tắt | độ sáng trung bình kênh Y < 40 | "Ánh sáng yếu" (flash tự động thì bật đèn, không nhắc) |
| 2 | Rung tay | gyroscope > 0.5 rad/s trong 500 ms gần nhất | "Giữ yên máy" |
| 3 | Ảnh mờ | phương sai Laplacian < `[ngưỡng cuối]` | "Ảnh mờ, giữ yên" |
| 4 | Chữ chạm mép | khung chữ cách mép < 5% | "Đưa máy ra xa" |
| 5 | Lệch tâm | tâm khung chữ x < 0.3 / > 0.7 | "Lệch trái" / "Lệch phải" |
| 6 | Chữ quá nhỏ | khung chữ < 10% diện tích ảnh | "Lại gần" |
| ✓ | Đạt hết + chữ ổn định 3/5 frame | — | rung ngắn 50 ms → đọc |

Không thấy chữ: lần đầu nhắc "Chưa thấy chữ" sau 5 s, sau đó cứ 10 s nhắc lại một lần. Lời nhắc thuộc mức GUIDANCE nên bị bộ điều phối giới hạn 12 lần / phút và không ngắt lời đang đọc.

**Cài đặt:**
- Độ sáng và độ nét tính trên kênh Y, lấy mẫu mỗi 4 điểm ảnh, không làm dịch buffer mà ML Kit còn dùng.
- Khung chữ là hình bao các `TextBlock` của ML Kit, quy về toạ độ ảnh đã xoay đúng chiều.

## 3. Đọc theo câu
Văn bản được tách theo dấu kết câu và xuống dòng; các mảnh quá ngắn (như "1.") được gộp vào câu bên cạnh. Lệnh "dừng" giữ lại câu đang đọc, "tiếp" đọc lại từ đầu câu đó, "đọc lại" đọc lại câu hiện tại.

## 4. Chế độ so sánh (baseline)
Tắt toàn bộ lời nhắc căn chỉnh và đọc cả đoạn mỗi khi chữ ổn định, giống cách VisionAid làm. Chế độ này dùng làm điều kiện A khi thử nghiệm.

## 5. Đánh giá
**Dữ liệu:** 30 mẫu (hộp thuốc, hóa đơn, sách) × 3 mức sáng (đủ sáng / phòng tối có flash / tối không flash). Mỗi mẫu có bản gõ tay làm chuẩn.

**Chỉ số** (từ `python eval/eval.py ocr`):

| Mức sáng | CER | WER | Tự chụp thành công |
|---|---|---|---|
| Đủ sáng | […] | […] | […] |
| Tối, có flash | […] | […] | […] |
| Tối, không flash | […] | […] | […] |

**Hiệu chỉnh ngưỡng:** ghi giá trị ban đầu và giá trị cuối của `BLUR_LAPLACIAN_MIN`, cùng lý do chỉnh `[…]`.

## 6. Hạn chế
- ML Kit nhận chữ Latin, hay sai dấu tiếng Việt; CER phản ánh cả sai dấu.
- Chưa đọc đúng thứ tự văn bản nhiều cột.
- Ngưỡng Laplacian phụ thuộc camera từng máy.

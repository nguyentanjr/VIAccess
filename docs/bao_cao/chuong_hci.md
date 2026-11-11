# Chương: Thiết kế lấy người dùng làm trung tâm và kết quả thử nghiệm (Hoàng)

> Bản nháp. Mọi chỗ `[…]` cần điền số liệu thật sau khi thử nghiệm; **không** điền số ước đoán.

## 1. Người dùng mục tiêu
**P1 – Người mù hoàn toàn.** Dùng điện thoại Android qua TalkBack, không nhìn màn hình; nhận thông tin bằng tai và cảm nhận rung. Cần: lệnh ngắn, phản hồi rõ ràng, không bị nói dồn dập lấn át âm thanh môi trường.

**P2 – Người khiếm thị một phần.** Nhìn được chữ rất to, màu tương phản cao. Cần: chữ ≥ 24sp, nút lớn, có thể kết hợp nhìn và nghe.

**Kịch bản:** S1 đọc hướng dẫn trên hộp thuốc ở phòng thiếu sáng · S2 đọc tổng tiền hóa đơn, có dừng / đọc lại · S3 tìm điện thoại trên bàn · S4 tìm cốc trong bếp có vật gây nhiễu.

## 2. Khảo sát giải pháp liên quan
Tóm tắt bảng so sánh ở `docs/khao_sat_giai_phap.md` (Seeing AI, Lookout, Envision, Be My Eyes, VisionAid). Ba điểm rút ra định hướng thiết kế: nói ít nhưng đúng lúc; hướng dẫn căn camera trước khi đọc; xử lý trên máy, có phương án chạm khi giọng nói không dùng được.

## 3. Vấn đề của bản kế thừa (VisionAid)
- Đọc liên tục mọi chữ / mọi vật → quá tải thính giác (nhận xét của giảng viên).
- Nhiều lời nói chồng nhau, không có thứ tự ưu tiên.
- Không hướng dẫn người dùng đưa camera về đúng vị trí; đọc cả khi ảnh mờ.
- Phụ thuộc lệnh giọng nói; khi nhận dạng giọng nói lỗi thì không có lối thoát.

## 4. Quyết định thiết kế
| Vấn đề | Quyết định | Cơ sở |
|---|---|---|
| Quá tải lời nói | Một hàng đợi, 4 mức ưu tiên, nghỉ 3 s cho cùng một câu, tối đa 12 lời nhắc / phút | Nhận xét GV; Lookout chỉ nói khi có thay đổi |
| Không biết căn camera | Mỗi lần nhắc **một** điều theo thứ tự: sáng → rung tay → mờ → chạm mép → lệch → nhỏ | Giảm tải nhận thức; chỉ báo điều quan trọng nhất |
| Tìm vật bằng giọng nói gây ồn | Rung theo khoảng cách, chỉ nói khi thấy / đổi hướng / mất dấu | Kênh xúc giác rảnh khi tai cần nghe môi trường |
| Văn bản dài | Đọc theo câu: dừng / tiếp / đọc lại / câu trước / sau | Người dùng kiểm soát nhịp |
| Giọng nói không ổn định | 5 nút chạm cố định ở cuối màn; không hiểu 3 lần → nhắc dùng nút | Nguyên tắc nhiều phương thức tương tác |
| Người khiếm thị một phần | Chữ ≥ 24sp, tương phản ≥ 7:1, vùng chạm ≥ 48dp, nhãn TalkBack | WCAG 2.1 AAA (1.4.6), Material accessibility |

Wireframe 3 màn và toàn bộ câu nói / mẫu rung: `docs/wireframe_kich_ban_am_thanh.md`.

## 5. Đánh giá heuristic (Nielsen) — tuần 5
Mỗi thành viên tự đánh giá độc lập theo 10 heuristic, gộp lỗi, xếp mức độ 0–4. Bảng kết quả: `[…]`.

## 6. Thử nghiệm người dùng
**Phương pháp:** within-subject A/B, `[n]` người (`[x]` người khiếm thị thật, `[y]` người bịt mắt), đổi thứ tự A/B. Chi tiết: `docs/thu_nghiem_nguoi_dung.md`.

**Kết quả** (điền từ `python eval/eval.py …`):

| Chỉ số | A – baseline | B – VIAccess | Wilcoxon p |
|---|---|---|---|
| Tỷ lệ hoàn thành | […] | […] | — |
| Thời gian TB (s) | […] | […] | […] |
| Số lỗi TB | […] | […] | […] |
| SUS | […] | […] | […] |
| NASA-TLX (raw) | […] | […] | […] |
| Câu nói / phút | […] | […] | […] |

**Phỏng vấn:** tóm tắt các ý lặp lại `[…]`.

## 7. Thảo luận và hạn chế
- Cỡ mẫu nhỏ; người bịt mắt không đại diện hoàn toàn cho người mù lâu năm.
- Thử nghiệm trong một buổi, chưa đo việc dùng lâu dài.
- `[…]` (điền sau khi có kết quả)

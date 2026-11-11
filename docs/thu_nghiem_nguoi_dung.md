# Kịch bản thử nghiệm người dùng (PLAN 6.3 — Hoàng)

## 1. Thiết kế
- **Người tham gia:** 6–8 người. Ưu tiên người khiếm thị; nếu thiếu, người sáng mắt bịt mắt (ghi rõ trong báo cáo là hạn chế).
- **Within-subject A/B:** mỗi người dùng cả hai bản.
  - **A – baseline:** Cài đặt → bật "Chế độ so sánh (bản cũ)".
  - **B – VIAccess:** tắt chế độ so sánh.
  - Đổi thứ tự: người số lẻ A→B, người số chẵn B→A.
- **Nhiệm vụ** (tối đa 120 s / nhiệm vụ, đo bằng đồng hồ bấm giờ):
  - S1 – Đọc hướng dẫn trên hộp thuốc ở phòng thiếu sáng.
  - S2 – Đọc tổng tiền trên hóa đơn; trong lúc đọc nói "dừng", rồi "đọc lại".
  - S3 – Tìm điện thoại đặt trên bàn.
  - S4 – Tìm cái cốc trong bếp, có 3 vật gây nhiễu xung quanh.
  - Dùng 2 bộ đạo cụ khác nhau cho A và B (hộp thuốc khác, vị trí vật khác) để tránh nhớ đáp án.
- **Đo:** hoàn thành (0/1), thời gian (s), số lỗi (nói lệnh sai, chạm sai, chọn nhầm vật), tìm đúng vật (0/1), SUS, NASA-TLX sau mỗi bản, phỏng vấn ngắn cuối buổi. Ghi vào `eval/templates/tasks.csv`, `sus.csv`, `tlx.csv`.
- **Log:** sau mỗi người, ở màn Tìm bấm "Xuất số liệu" lưu CSV (đặt tên `P01_A.csv`…).

## 2. Quy trình một buổi (~40 phút)
1. Chào, giải thích mục đích, đọc phiếu đồng ý, ký (5').
2. Làm quen app 5': nghe hướng dẫn, thử "đọc chữ", "tìm cốc", nút chạm.
3. Bản thứ nhất: S1–S4 → SUS + NASA-TLX (12').
4. Nghỉ 2'.
5. Bản thứ hai: S1–S4 (đạo cụ bộ 2) → SUS + NASA-TLX (12').
6. Phỏng vấn 3 câu (5').

Người điều phối chỉ đọc lời dẫn đã soạn, **không gợi ý** cách làm trong lúc đo. Nếu người tham gia kẹt quá 120 s → ghi "không hoàn thành", chuyển nhiệm vụ.

## 3. Phiếu đồng ý tham gia
> Tôi đồng ý tham gia thử nghiệm ứng dụng VIAccess do Nhóm 13, môn Tương tác người máy (INT2041), Trường ĐH Công nghệ – ĐHQGHN thực hiện.
> - Tôi được giải thích mục đích: đánh giá mức dễ dùng của ứng dụng hỗ trợ đọc chữ và tìm đồ vật.
> - Buổi thử kéo dài khoảng 40 phút. Tôi có thể dừng bất kỳ lúc nào mà không cần giải thích.
> - Nhóm **không ghi hình, không ghi âm, không lưu ảnh** của tôi. Chỉ lưu thời gian, số lỗi, câu trả lời phiếu và nhật ký ẩn danh của ứng dụng, gắn với mã người tham gia (P01, P02…), không gắn với tên.
> - Dữ liệu chỉ dùng cho báo cáo môn học và được xóa sau khi kết thúc học phần.
>
> Mã người tham gia: ____ Ngày: ____ Chữ ký (hoặc đồng ý bằng lời, người điều phối ghi lại): ____

Với người khiếm thị: người điều phối đọc to toàn bộ phiếu, ghi nhận đồng ý bằng lời và ký thay "đã đọc cho người tham gia nghe".

## 4. SUS (System Usability Scale) — 1 = Rất không đồng ý … 5 = Rất đồng ý
1. Tôi nghĩ tôi sẽ muốn dùng ứng dụng này thường xuyên.
2. Tôi thấy ứng dụng phức tạp không cần thiết.
3. Tôi thấy ứng dụng dễ dùng.
4. Tôi nghĩ tôi cần người kỹ thuật giúp mới dùng được ứng dụng này.
5. Tôi thấy các chức năng của ứng dụng được kết hợp tốt với nhau.
6. Tôi thấy ứng dụng có quá nhiều điểm không nhất quán.
7. Tôi nghĩ hầu hết mọi người sẽ học dùng ứng dụng này rất nhanh.
8. Tôi thấy ứng dụng rất rườm rà khi dùng.
9. Tôi thấy tự tin khi dùng ứng dụng.
10. Tôi cần học nhiều thứ trước khi dùng được ứng dụng này.

Điểm: câu lẻ (điểm − 1), câu chẵn (5 − điểm), cộng lại × 2,5 → 0–100 (`eval.py sus` tính tự động).

## 5. NASA-TLX (bản Raw, 0 = rất thấp … 100 = rất cao, bước 5)
| Thang | Câu hỏi đọc cho người tham gia |
|---|---|
| Tải tinh thần | Nhiệm vụ đòi hỏi bạn suy nghĩ, ghi nhớ, tập trung nhiều đến mức nào? |
| Tải thể chất | Bạn phải cử động, cầm máy, xoay người nhiều đến mức nào? |
| Áp lực thời gian | Bạn thấy bị vội, bị thúc ép về thời gian đến mức nào? |
| Hiệu quả | Bạn thấy mình làm *không* tốt đến mức nào? (0 = rất tốt, 100 = rất kém) |
| Nỗ lực | Bạn phải cố gắng nhiều đến mức nào để đạt kết quả đó? |
| Bực bội | Bạn thấy khó chịu, căng thẳng, bực mình đến mức nào? |

## 6. Phỏng vấn 3 câu
1. Lúc nào bạn thấy ứng dụng nói hoặc rung **quá nhiều / quá ít**?
2. Giữa hai bản, bạn muốn dùng bản nào hằng ngày? Vì sao?
3. Nếu được sửa một điều, bạn sẽ sửa gì?

## 7. Pilot 1 người
Chạy toàn bộ quy trình với 1 người trước. Ghi lại: lời dẫn chỗ nào gây hiểu nhầm, nhiệm vụ nào quá dễ / quá khó, thời gian thực tế. Sửa tài liệu này rồi mới làm với 6–8 người. Kết quả pilot **không** đưa vào số liệu chính.

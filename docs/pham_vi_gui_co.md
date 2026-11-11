# Phạm vi đồ án đã thu gọn — gửi cô duyệt (Tân)

**Nhóm 13 – INT2041 Tương tác người máy**
Nguyễn Đình Nhật Tân · Phạm Anh Tiến · Nguyễn Thế Hùng · Đường Minh Hoàng

Kính gửi cô,

Sau góp ý của cô về việc ứng dụng phản hồi quá nhiều và phạm vi quá rộng, nhóm em xin gửi phạm vi đã thu gọn của đề tài **VIAccess – ứng dụng Android hỗ trợ người khiếm thị đọc văn bản và tìm đồ vật**. Nhóm kế thừa mã nguồn VisionAid (Nhóm 2, khóa 2025–2026) và sẽ ghi rõ nguồn trong báo cáo.

## 1. Phạm vi
**Giữ lại hai tác vụ chính:**
- **T1 – Đọc văn bản:** ứng dụng hướng dẫn người dùng căn camera, tự chụp khi ảnh đạt yêu cầu, rồi đọc theo từng câu. Người dùng có thể dừng, đọc tiếp, đọc lại.
- **T2 – Tìm vật thể:** người dùng chọn 1 trong 6 vật (cốc, chai, điện thoại, điều khiển, sách, ghế). Ứng dụng rung nhanh dần khi lại gần và nói ngắn gọn: đã thấy, hướng trái / giữa / phải, mất dấu.

**Hỗ trợ:** 5 lệnh giọng nói, mỗi lệnh có nút chạm thay thế; chữ to, tương phản cao, nhãn cho TalkBack; chỉ tiếng Việt.

**Bỏ khỏi bản kế thừa:** Explore (tự đọc mọi vật), nhận diện tiền (cần mạng và API key), Lịch sử, Giao diện, tiếng Anh.
**Không làm:** mô tả cảnh, dẫn đường, ước lượng khoảng cách bằng mét, huấn luyện mô hình mới.

## 2. Quy tắc phản hồi chống quá tải (trả lời góp ý của cô)
- Mọi lời nói và rung đi qua **một bộ điều phối** có 4 mức ưu tiên: lỗi > thay đổi trạng thái > hướng dẫn căn chỉnh > thông tin phụ. Lời mới chỉ ngắt lời cũ khi quan trọng hơn.
- Cùng một câu không lặp lại trong 3 giây; tối đa 12 lời nhắc căn chỉnh mỗi phút (6 lời ở chế độ "Ngắn gọn").
- Khi căn camera, mỗi lần chỉ nhắc **một** điều, theo thứ tự: thiếu sáng → rung tay → ảnh mờ → chữ chạm mép → lệch tâm → chữ quá nhỏ.
- Khi tìm vật: chỉ nói lúc thấy, lúc đổi hướng (cách nhau ≥ 2 giây) và lúc mất dấu; khoảng cách được báo bằng nhịp rung chứ không bằng lời.
- Khi đang nói thì không rung dày và tạm không nghe lệnh, để tránh mic nghe lại chính lời của ứng dụng.

## 3. Đánh giá
- **Kỹ thuật:** precision / recall / F1 nhận diện trên 150 ảnh tự chụp (6 lớp, 2 mức sáng); số lần báo sai khi có / không có luật xác nhận 3 frame; CER / WER OCR trên 30 mẫu × 3 mức sáng; độ trễ suy luận và độ trễ từ frame đến phản hồi.
- **Người dùng:** 6–8 người (ưu tiên người khiếm thị thật; nếu thiếu thì dùng người bịt mắt và nêu rõ hạn chế). Mỗi người làm 4 nhiệm vụ trên cả hai bản: A = cách phản hồi của VisionAid, B = VIAccess. Đo tỷ lệ hoàn thành, thời gian, số lỗi, SUS, NASA-TLX; kiểm định Wilcoxon.
- **Đạo đức:** có phiếu đồng ý; không lưu ảnh hoặc giọng nói của người tham gia; chỉ lưu nhật ký ẩn danh.

Nhóm em mong cô góp ý về phạm vi trên và cho nhóm biết hạn nộp báo cáo, ngày thuyết trình.

Em cảm ơn cô.
Đại diện nhóm: Nguyễn Đình Nhật Tân

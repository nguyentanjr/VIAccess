# Checklist kiểm thử chức năng (PLAN 6.1 — Tân chủ trì)

Máy test: `[model, Android x]` · Bản build: `[commit]` · Người test: `[…]` · Ngày: `[…]`
Ghi **Đạt / Không đạt / Không thử được** và ghi chú. "≤ 1 s" đo bằng mắt + đồng hồ, hoặc từ log `VIA_METRIC`.
Cột "Máy ảo?": thử được trên máy ảo (webcam, mic laptop) hay bắt buộc máy thật.

## A. Khởi động, quyền, lỗi
| # | Bước | Kết quả mong đợi | Máy ảo? | KQ |
|---|---|---|---|---|
| A1 | Cài mới, mở app | Nghe lời chào hỏi mức thị lực; 2 nút lớn | Có | |
| A2 | Nói "một phần" | Nghe xác nhận, sang màn hướng dẫn | Có | |
| A3 | Chạm màn hướng dẫn, từ chối quyền | Màn "Cần quyền camera và micro", nghe lý do, nút lớn "Cấp quyền" | Có | |
| A4 | Từ chối 2 lần rồi chạm nút | Nút đổi thành "Mở cài đặt ứng dụng", mở đúng trang cài đặt app | Có | |
| A5 | Cấp quyền trong cài đặt, quay lại | Vào thẳng màn chính | Có | |
| A6 | Gỡ giọng tiếng Việt của TTS, mở app | Rung đôi + hộp thoại "Máy chưa có giọng đọc tiếng Việt", nút cài | Có | |
| A7 | Bật chế độ máy bay, nói một lệnh | Nghe "Mất kết nối mạng…" ≤ 1 lần / phút; nút chạm vẫn dùng được | Có | |
| A8 | App đang nói, bấm Home | Im ngay, không rung; mở lại thì nghe lệnh tiếp | Có | |

## B. Lệnh giọng nói và nút chạm (4.4)
| # | Bước | Kết quả mong đợi | Máy ảo? | KQ |
|---|---|---|---|---|
| B1 | Ở màn Tìm, nói "đọc chữ" | Sang màn Đọc chữ | Có | |
| B2 | Ở màn Đọc chữ, nói "tìm cốc" | Sang màn Tìm, nghe "Đang tìm cốc" | Có | |
| B3 | Nói "quay lại" ở màn Đọc chữ | Về màn Tìm | Có | |
| B4 | Đang đọc văn bản dài, nói "dừng" | Im ngay, rung ngắn | Có | |
| B5 | Nói "tiếp" | Đọc tiếp từ câu đang dở | Có | |
| B6 | Nói "đọc lại" | Đọc lại câu hiện tại | Có | |
| B7 | Ở màn Đọc chữ, nói câu vô nghĩa 3 lần | Lần 1, 2: "Không hiểu, hãy nói lại"; lần 3: nhắc dùng nút chạm | Có | |
| B8 | Nói "giúp đỡ" | Nghe màn hiện tại + danh sách lệnh | Có | |
| B9 | Lần lượt chạm 5 nút ở thanh dưới | Mỗi nút rung ngắn và làm đúng việc; nút Dừng đổi thành Tiếp khi đang dừng | Có | |
| B10 | Bật TalkBack, vuốt qua thanh dưới | Mỗi nút được đọc tên | Có | |

## C. Đọc chữ (4.2)
| # | Bước | Kết quả mong đợi | Máy ảo? | KQ |
|---|---|---|---|---|
| C1 | Flash = Tắt, che sáng | "Ánh sáng yếu" trong ≤ 1 s | Không | |
| C2 | Flash = Tự động, phòng tối | Đèn flash bật, không nhắc "Ánh sáng yếu" | Không | |
| C3 | Lắc máy | "Giữ yên máy" | Có (cảm biến ảo) | |
| C4 | Để lệch nét | "Ảnh mờ, giữ yên" | Không (webcam tự lấy nét) | |
| C5 | Đưa tờ giấy sát camera | "Đưa máy ra xa" | Có | |
| C6 | Chữ lệch trái / phải | "Lệch trái" / "Lệch phải" | Có | |
| C7 | Chữ nhỏ ở xa | "Lại gần" | Có | |
| C8 | Chĩa vào tường trơn | "Chưa thấy chữ" sau 5 s, nhắc lại mỗi 10 s | Có | |
| C9 | Giữ yên chữ ở giữa | Rung ngắn rồi đọc theo câu | Có | |
| C10 | Cùng văn bản giữ tiếp | Không đọc lặp lại ngay | Có | |
| C11 | Bật Chế độ so sánh | Không nhắc căn chỉnh, đọc liên tục | Có | |
| C12 | Mức chi tiết = Ngắn gọn | Số lời nhắc mỗi phút giảm rõ | Có | |

## D. Tìm vật (4.3)
| # | Bước | Kết quả mong đợi | Máy ảo? | KQ |
|---|---|---|---|---|
| D1 | Chọn "Cốc", chưa có cốc | Rung nhẹ mỗi 2 s, không nói | Không (rung) | |
| D2 | Đưa cốc vào khung | Sau ≥ 3 frame: "Đã thấy cốc, <hướng>" rồi rung dài | Có (nghe) | |
| D3 | Đưa cốc từ trái sang phải | Hướng được báo khi đổi vùng, cách nhau ≥ 2 s | Có | |
| D4 | Giữ cốc ở giữa ≥ 1 s | "Ở phía trước" đúng 1 lần | Có | |
| D5 | Lại gần cốc | Rung dày dần (250–1000 ms) | Không | |
| D6 | Cất cốc đi > 1,5 s | "Mất dấu" 1 lần rồi rung đôi | Có (nghe) | |
| D7 | Có chai cạnh cốc, đang tìm cốc | Không báo chai | Có | |
| D8 | Bấm "Xuất số liệu" | Lưu được file CSV, mở ra có các dòng VIA_METRIC | Có | |

## E. Heuristic Nielsen (đánh giá nội bộ, tuần 5)
Mỗi người tự đi qua S1–S4, ghi vấn đề theo 10 heuristic, mức độ 0–4:
1 Hiển thị trạng thái · 2 Khớp với thế giới thực · 3 Kiểm soát và tự do · 4 Nhất quán · 5 Phòng lỗi · 6 Nhận biết hơn nhớ · 7 Linh hoạt · 8 Tối giản · 9 Giúp nhận ra và sửa lỗi · 10 Trợ giúp.
Gộp vào một bảng chung: `#, heuristic, mô tả, màn, mức độ, người phát hiện, cách sửa`.

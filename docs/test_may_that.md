# Những gì phải test trên điện thoại thật

Máy ảo đã thử được giao diện, lệnh giọng nói, đọc chữ, tìm vật, Khám phá, QR. Danh sách dưới đây là những thứ **máy ảo không thử được** hoặc cho kết quả không đáng tin. Danh sách đầy đủ ở `docs/checklist_kiem_thu.md`.

Ghi lại trước khi test: **model máy, phiên bản Android, commit đang cài** (`git log --oneline -1`). Ghi vào PLAN mục 9.1.

## Chuẩn bị
- Cài: cắm USB, bật *Gỡ lỗi USB*, chạy `./gradlew installDebug`.
- Tải giọng **Tiếng Việt** trong Cài đặt → Chuyển văn bản thành giọng nói.
- Đạo cụ: hộp thuốc, hóa đơn, 1 trang sách; cốc, chai, điện thoại, điều khiển, sách, ghế; 1 mã QR in giấy + 1 mã VietQR trên điện thoại khác.
- Xem log trong lúc test (máy tính cắm cáp):
  ```bash
  adb logcat -s VIA_METRIC VIA_QR
  ```

## 1. Rung (máy ảo không rung)
| # | Làm | Đúng khi | KQ |
|---|---|---|---|
| R1 | Chạm từng nút ở thanh dưới | Mỗi lần chạm rung ngắn | |
| R2 | Chọn Cốc, chưa có cốc | Rung rất nhẹ mỗi ~2 s, không nói | |
| R3 | Đưa cốc vào khung | Nói "Đã thấy cốc, …" rồi **rung dài** | |
| R4 | Tiến lại gần cốc | Nhịp rung **dày dần** (xa ~1 s, sát ~0,25 s) | |
| R5 | Cất cốc đi > 1,5 s | Nói "Mất dấu" rồi **rung đôi** | |
| R6 | Đọc chữ, chữ ổn định | Rung ngắn rồi mới đọc | |
| R7 | Đang nói thì không rung dày (chỉ rung xác nhận) | Không rung đè lên lời nói | |
| R8 | Cài đặt → tắt Rung | Không còn rung ở mọi bước trên | |

## 2. Đèn flash và ánh sáng
| # | Làm | Đúng khi | KQ |
|---|---|---|---|
| F1 | Flash = Tự động, vào phòng tối | Đèn tự bật; không nói "Ánh sáng yếu" | |
| F2 | Bật lại đèn phòng | Đèn flash tự tắt (không nháy bật/tắt liên tục) | |
| F3 | Flash = Tắt, phòng tối, màn Đọc chữ | Nói "Ánh sáng yếu" trong ≤ 1 s | |
| F4 | Flash = Bật | Đèn luôn sáng khi ở màn Tìm / Đọc chữ | |

## 3. Ngưỡng căn camera (chỉnh số trong `FeedbackConfig.kt`)
| # | Làm | Đúng khi | Nếu sai thì chỉnh | KQ |
|---|---|---|---|---|
| N1 | Giữ yên, chữ rõ | **Không** nói "Ảnh mờ" | Giảm `BLUR_LAPLACIAN_MIN` (đang 100) | |
| N2 | Để lệch nét (quá gần, dưới 10 cm) | Nói "Ảnh mờ, giữ yên" | Tăng `BLUR_LAPLACIAN_MIN` | |
| N3 | Cầm máy bình thường | **Không** nói "Giữ yên máy" | Tăng `GYRO_SHAKE_RAD_S` (đang 0.5) | |
| N4 | Lắc nhẹ tay | Nói "Giữ yên máy" | Giảm `GYRO_SHAKE_RAD_S` | |
| N5 | Hộp thuốc ở giữa, cách ~20 cm | Rung + đọc trong ≤ 3 s | Xem lời nhắc nào chặn | |

Ghi giá trị cuối cùng vào PLAN mục 4.5.

## 4. Tốc độ, pin, nhiệt (máy ảo chạy CPU máy tính nên số không đúng)
| # | Làm | Đúng khi | KQ |
|---|---|---|---|
| T1 | Màn Tìm 1 phút, xem `inference_complete` trong log | Trung bình ≤ 200 ms | ___ ms |
| T2 | Từ lúc giơ vật đến lúc nghe "Đã thấy" | ≤ 1 s (3 frame xác nhận) | ___ s |
| T3 | Để màn Tìm chạy 10 phút | Máy không nóng quá tay, không giật | |
| T4 | Ghi % pin trước / sau 10 phút | Ghi lại để đưa vào báo cáo | ___ % |

Nếu T1 > 200 ms hoặc máy nóng: tăng `YOLO_FRAME_SKIP` từ 2 lên 3.

## 5. Âm thanh và giọng nói thật
| # | Làm | Đúng khi | KQ |
|---|---|---|---|
| A1 | Nói "đọc chữ", "tìm cốc", "quay lại" ở phòng yên tĩnh | Đúng 5/5 lần | |
| A2 | Như A1 nhưng có tiếng ồn (quạt, nhạc nhỏ) | Ghi số lần đúng | __/5 |
| A3 | App đang đọc, nói "dừng" | Dừng ngay, không bị mic nghe lại lời app | |
| A4 | Tắt wifi + 4G, nói lệnh | Nghe báo mất mạng; nút chạm vẫn dùng được | |
| A5 | Bật TalkBack, vuốt qua các nút | Mỗi nút được đọc đúng tên, chạm đúp chọn được | |

## 6. Đời thực (camera thật, không phải webcam)
| # | Làm | Đúng khi | KQ |
|---|---|---|---|
| D1 | S1: đọc hộp thuốc ở phòng thiếu sáng | Đọc được, ghi thời gian | ___ s |
| D2 | S2: đọc tổng tiền hóa đơn, nói "dừng" rồi "đọc lại" | Làm được | |
| D3 | S3: tìm điện thoại trên bàn | Tìm được, ghi thời gian | ___ s |
| D4 | S4: tìm cốc trong bếp, có chai / bát bên cạnh | Không báo nhầm sang vật khác | |
| D5 | Quét QR in giấy và VietQR | Đọc đúng nội dung / tên / số tiền | |
| D6 | "Xung quanh có gì?" trong phòng | Đọc ≤ 3 vật, đúng hướng | |
| D7 | Đang dùng thì bấm Home, rồi mở lại | Im ngay; mở lại dùng tiếp được | |

## Sau khi test
- Gửi lỗi kèm: bước nào, mong đợi gì, thấy gì, đoạn log nếu có.
- Xuất log: Cài đặt → **Xuất số liệu (CSV)** → để vào `eval/` trên drive nhóm (dùng cho `eval/eval.py latency`).
- Tick PLAN 9.1 "Build + chạy trên máy thật", ghi model máy.

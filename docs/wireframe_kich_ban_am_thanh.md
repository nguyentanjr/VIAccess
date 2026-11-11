# Wireframe 3 màn + kịch bản âm thanh (PLAN 9.4 — Hoàng)

Nguyên tắc chung: nền tối, chữ trắng / vàng (tương phản ≥ 7:1), chữ ≥ 24sp, vùng chạm ≥ 48dp.
Thanh nút cố định ở cuối mọi màn: **Tìm · Đọc chữ · Dừng/Tiếp · Đọc lại · Cài đặt** (thay cho 5 lệnh giọng nói).

## 1. Màn Tìm vật
```
┌──────────────────────────────┐
│ [Chế độ so sánh ◯] [Xuất số liệu] │  ← chỉ dùng khi thử nghiệm
│ Đang tìm cốc                  │  ← trạng thái, chữ lớn
│                              │
│        (camera + khung vật)  │
│                              │
├──────────────────────────────┤
│ [ Cốc ]        [ Chai ]       │
│ [ Điện thoại ] [ Điều khiển ] │  ← 6 nút chọn vật, mỗi nút ≥ 48dp
│ [ Sách ]       [ Ghế ]        │
├──────────────────────────────┤
│ Tìm │ Đọc chữ │ Dừng │ Đọc lại │ Cài đặt │
└──────────────────────────────┘
```

## 2. Màn Đọc chữ
```
┌──────────────────────────────┐
│                              │
│        (camera)              │  ← chạm bất kỳ đâu = đọc lại văn bản hiện tại
│                              │
├──────────────────────────────┤
│ Văn bản vừa đọc (24sp, nền   │
│ đen mờ)                      │
├──────────────────────────────┤
│ Tìm │ Đọc chữ │ Dừng │ Đọc lại │ Cài đặt │
└──────────────────────────────┘
```

## 3. Màn Cài đặt
```
┌──────────────────────────────┐
│ Cài đặt                       │
│ Tốc độ nói: 1.0x              │
│ [ Chậm hơn ]   [ Nhanh hơn ]  │
│ Rung                    [●]   │
│ Đèn chớp: [Tự động][Bật][Tắt] │
│ Mức chi tiết: [Đầy đủ][Ngắn gọn] │
│ Chế độ so sánh (bản cũ) [◯]   │
│ [ Khôi phục cài đặt gốc ]     │  ← chạm 7 lần
└──────────────────────────────┘
```

## Kịch bản âm thanh

### Lời nói (theo mức ưu tiên của bộ điều phối)
| Mức | Khi nào | Câu nói |
|---|---|---|
| ALERT | Thiếu quyền | "Cần quyền camera và micro. Ứng dụng cần camera để nhìn chữ và đồ vật, và micro để nghe lệnh…" |
| ALERT | Mất mạng (tối đa 1 lần / phút) | "Mất kết nối mạng nên chưa nghe được lệnh giọng nói. Hãy dùng nút chạm ở cuối màn hình." |
| ALERT | Lỗi mô hình | "Không tải được mô hình nhận diện vật. Chức năng tìm vật tạm thời không dùng được." |
| STATE | Mở màn Tìm | "Chế độ tìm kiếm" → "Bạn đang tìm gì? …" |
| STATE | Chọn vật | "Đang tìm cốc" |
| STATE | Thấy vật (3 frame liên tiếp) | "Đã thấy cốc, bên trái / ở giữa / bên phải" |
| STATE | Vào giữa ≥ 1 s | "Ở phía trước" |
| STATE | Mất > 1,5 s | "Mất dấu" |
| STATE | Đọc chữ | đọc từng câu của văn bản |
| STATE | Lệnh không hiểu | "Không hiểu, hãy nói lại" (lần 3: nhắc dùng nút chạm) |
| GUIDANCE | Căn camera | "Ánh sáng yếu" · "Giữ yên máy" · "Ảnh mờ, giữ yên" · "Đưa máy ra xa" · "Lệch trái" · "Lệch phải" · "Lại gần" |
| GUIDANCE | Không thấy chữ 5 s (rồi mỗi 10 s) | "Chưa thấy chữ" |
| TRACKING | Hướng thay đổi (cách nhau ≥ 2 s) | "Bên trái" / "Ở giữa" / "Bên phải" |

### Mẫu rung
| Mẫu | Thời lượng | Ý nghĩa |
|---|---|---|
| Ngắn | 50 ms | Xác nhận lệnh / chạm nút; chữ đã ổn định, sắp đọc |
| Mặc định | 100 ms | Chuyển màn |
| Dài | 300 ms | Đã thấy vật |
| Đôi | 100–100–100 ms | Mất dấu; báo lỗi |
| Nhịp đang tìm | ngắn mỗi 2 s | Đang tìm, chưa thấy |
| Nhịp khoảng cách | chu kỳ 1000 − 750 × (diện tích khung / ảnh), kẹp 250–1000 ms | Càng gần càng rung dày |

Không rung dày khi đang nói (trừ rung xác nhận).

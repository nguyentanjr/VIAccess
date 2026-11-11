# eval/ — tổng hợp số liệu đánh giá

Script `eval.py` chỉ dùng thư viện chuẩn Python 3.8+. Mẫu cột của từng file ở `templates/`.
Dữ liệu thật (ảnh, log, phiếu) để trên drive nhóm, **không** đẩy lên git; chỉ đẩy bảng kết quả cuối.

| Lệnh | File vào | Ra | PLAN |
|---|---|---|---|
| `python eval.py detection data/detection.csv` | mỗi ảnh: `gt` (lớp đúng / `none`), `pred` (lớp app báo / `none`), `light` | P / R / F1 từng lớp + macro, theo mức sáng | 6.2 – Hùng |
| `python eval.py ocr data/ocr.csv` | `reference` (gõ tay), `hypothesis` (app đọc), `light`, `auto_captured` | CER, WER, tỷ lệ tự chụp thành công | 6.2 – Tiến |
| `python eval.py latency data/metrics.csv` | CSV xuất từ nút "Xuất số liệu" ở màn Tìm (MetricLogger) | thời gian suy luận, frame → phản hồi, câu nói / lần rung mỗi phút | 6.2 – Hùng |
| `python eval.py sus data/sus.csv` | `participant, condition (A/B), q1..q10` | điểm SUS + Wilcoxon A/B | 6.3 – Hoàng |
| `python eval.py tlx data/tlx.csv` | 6 thang NASA-TLX (0–100) | Raw TLX + Wilcoxon A/B | 6.3 – Hoàng |
| `python eval.py ab data/tasks.csv --metric time_s` | `completed, time_s, errors` mỗi nhiệm vụ | trung bình theo người, tỷ lệ hoàn thành, Wilcoxon | 6.3 – Hoàng |

Quy ước: **A** = baseline (bật "Chế độ so sánh" trong Cài đặt), **B** = VIAccess.
Đo false positive "có / không luật 3 frame": chạy cùng 20 lượt tìm với chế độ so sánh tắt (có luật) và bật (không luật), đếm `find_found` / `baseline_detection` trên khung không có vật.

Wilcoxon: n ≤ 20 và không có hạng trùng → p chính xác; còn lại dùng xấp xỉ chuẩn. Với 6–8 người, báo cáo kèm trung vị và cỡ mẫu, không chỉ p.

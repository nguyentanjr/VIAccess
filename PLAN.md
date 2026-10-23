# PLAN — VIAccess (Nhóm 13, INT2041)

> Đây là nguồn sự thật duy nhất về phạm vi, thiết kế, tiến độ và trạng thái.
> Cập nhật checkbox ngay khi xong việc. Ghi ngày bắt đầu và hạn nộp vào mục 0.

## 0. Thông tin chung
- **Hạn nộp:** `____` · **Ngày thuyết trình/demo:** `____`
- **Thành viên:** Nguyễn Đình Nhật Tân (**Tân**) · Phạm Anh Tiến (**Tiến**) · Nguyễn Thế Hùng (**Hùng**) · Đường Minh Hoàng (**Hoàng**)
- **Nền tảng:** kế thừa repo VisionAid (Nhóm 2, INT2041 2025–2026). Phải ghi nguồn trong báo cáo.

## 1. Bài toán & phạm vi

### 1.1 Mục tiêu
Người khiếm thị dùng điện thoại Android (camera) để **(1) đọc văn bản** và **(2) tìm một vật thể quen thuộc** mà không phải nhìn màn hình. Phản hồi bằng giọng nói và rung, có quy tắc để tránh quá tải.

### 1.2 Trong phạm vi
| # | Thành phần | Mô tả |
|---|---|---|
| T1 | **Đọc văn bản** | Hỗ trợ căn camera → tự chụp → OCR → TTS; dừng / tiếp / đọc lại |
| T2 | **Tìm vật thể** | Chọn 1 trong 6 vật → rung theo khoảng cách + giọng nói báo hướng / đã thấy / mất dấu |
| H1 | Giọng nói | 5 lệnh + phương án chạm |
| H2 | TTS & rung | Kênh phản hồi chung, có bộ điều phối chống quá tải |
| H3 | Accessibility | Chữ to, tương phản cao, nút lớn, nhãn TalkBack (yêu cầu phi chức năng) |

**6 vật mục tiêu (lớp COCO):** cup (cốc), bottle (chai), cell phone (điện thoại), remote (điều khiển), book (sách), chair (ghế).

### 1.3 Ngoài phạm vi
Explore tự đọc mọi vật · nhận diện tiền · History · Themes · mô tả cảnh · dẫn đường ngoài trời · đọc nhiều cột theo thứ tự · ước lượng khoảng cách bằng mét · huấn luyện mô hình mới · tiếng Anh (chỉ làm tiếng Việt).

### 1.4 Kế thừa và tự phát triển (đề bài yêu cầu làm rõ)
| Kế thừa từ VisionAid / thư viện | Nhóm tự phát triển hoặc điều chỉnh |
|---|---|
| CameraX pipeline, YOLOv5s TFLite, ML Kit OCR | Bộ điều phối phản hồi (ưu tiên, khoảng nghỉ, phân kênh) |
| TTS, SpeechRecognizer, Vibrator wrapper | Phát hiện mờ (Laplacian), rung tay (gyroscope), vị trí khung chữ |
| Auto-flash theo độ sáng, OCR ổn định 3/5 frame | Luật xác nhận 3 frame + conf ≥ 0.5; hướng trái/giữa/phải; mất dấu |
| Persistence 5 frame, debounce | Rung theo khoảng cách (điều chỉnh khoảng 250–1000 ms) |
| | Điều khiển dừng / tiếp / đọc lại |
| | Hệ thống log đo chỉ số + bộ dữ liệu test + thử nghiệm A/B |

## 2. Người dùng & kịch bản
- **P1 – Người mù hoàn toàn:** dùng TalkBack, chỉ nghe và cảm nhận rung.
- **P2 – Người khiếm thị một phần:** nhìn được chữ rất to và màu tương phản.
- **S1:** Đọc hướng dẫn trên hộp thuốc ở phòng thiếu sáng (T1).
- **S2:** Đọc tổng tiền trên hóa đơn (T1, có dừng / đọc lại).
- **S3:** Tìm chiếc điện thoại đặt trên bàn (T2).
- **S4:** Tìm cái cốc trong bếp, có vật gây nhiễu xung quanh (T2, kiểm tra báo sai).

## 3. Luồng tương tác
```
Mở app → TTS "Nói 'đọc chữ' hoặc 'tìm' + tên vật"
  ├─ "đọc chữ" → T1: nhắc căn camera → [ổn định] rung ngắn → đọc
  │       "dừng" / chạm 2 lần = tạm dừng · "đọc lại" · "quay lại"
  └─ "tìm cốc" → T2: rung theo khoảng cách → "Đã thấy cốc, bên trái"
          → [vào giữa] rung dày → [mất >1,5 s] "Mất dấu" · "quay lại"
```
Mỗi màn có **một nút lớn** chiếm nửa dưới màn hình để thay cho lệnh giọng nói.

## 4. Quy tắc phản hồi (trả lời nhận xét của cô)

### 4.1 Bộ điều phối chung (`FeedbackCoordinator`)
- **Một hàng đợi** cho mọi lời nói. Lời nói mới **có độ ưu tiên cao hơn** thì ngắt lời cũ; **thấp hơn hoặc bằng** thì bị bỏ.
- Mức ưu tiên: lỗi/an toàn > thay đổi trạng thái > hướng dẫn căn chỉnh > thông tin phụ.
- Cùng một nội dung không lặp lại trong `REPEAT_COOLDOWN_MS = 3000`.
- Tối đa `MAX_PROMPTS_PER_MIN = 12` lời nhắc căn chỉnh mỗi phút.
- Khi TTS đang nói: không phát rung dày và tạm dừng nghe lệnh giọng nói (đã có buffer 500 ms).

### 4.2 Căn camera (T1)
Mỗi lần chỉ nhắc **1 điều**, xét lần lượt từ trên xuống:

| Ưu tiên | Điều kiện | Ngưỡng ban đầu | Phản hồi |
|---|---|---|---|
| 1 | Thiếu sáng | luminance < 40 (vùng trễ 40/120) | Bật flash; nếu flash tắt: "Ánh sáng yếu" |
| 2 | Rung tay | độ lớn gyroscope > 0.5 rad/s trong 500 ms | "Giữ yên máy" |
| 3 | Ảnh mờ | phương sai Laplacian < 100 (cần hiệu chỉnh) | "Ảnh mờ, giữ yên" |
| 4 | Chữ chạm mép | khung chữ cách mép < 5% | "Đưa máy ra xa" |
| 5 | Lệch tâm | tâm khung chữ x < 0.3 / > 0.7 | "Lệch trái" / "Lệch phải" |
| 6 | Chữ quá nhỏ | khung chữ < 10% diện tích ảnh | "Lại gần" |
| ✓ | Đạt hết + ổn định 3/5 frame | — | Rung ngắn 50 ms → đọc |

- Không có chữ trong 5 s: nói "Chưa thấy chữ" một lần, sau đó nhắc lại mỗi 10 s.
- Văn bản được tách theo dòng hoặc câu để hỗ trợ dừng / tiếp / đọc lại.

### 4.3 Tìm vật thể (T2)
| Trạng thái | Điều kiện vào | Phản hồi |
|---|---|---|
| SEARCHING | Bắt đầu, hoặc sau LOST | Rung rất nhẹ mỗi 2 s ("đang tìm"), không nói |
| FOUND | Đúng lớp, ≥ 3 frame liên tiếp, conf ≥ 0.5 | Rung dài 300 ms + "Đã thấy <vật>, <hướng>" |
| TRACKING | Sau FOUND | Rung lặp theo khoảng cách: chu kỳ = 1000 − 750·(diện tích bbox / diện tích ảnh), kẹp trong [250, 1000] ms. Chỉ nói hướng khi vùng thay đổi, cách nhau ≥ 2 s |
| CENTERED | Tâm bbox trong vùng giữa ≥ 1 s | "Ở phía trước" (một lần) |
| LOST | > 1,5 s không thấy | Rung đôi + "Mất dấu" (một lần) → SEARCHING |

Vùng hướng: x < 0.35 = trái · 0.35–0.65 = giữa · > 0.65 = phải.

### 4.4 Lệnh giọng nói
| Lệnh | Từ khóa (chứa) | Hành động |
|---|---|---|
| Đọc chữ | "đọc chữ", "văn bản" | Vào T1 |
| Tìm | "tìm" + tên vật | Vào T2 với mục tiêu tương ứng |
| Dừng / tiếp | "dừng", "tiếp" | Tạm dừng / tiếp tục TTS |
| Đọc lại | "đọc lại", "lặp lại" | Đọc lại câu hiện tại |
| Quay lại | "quay lại" | Về màn chính |
Không nhận ra lệnh: "Không hiểu, hãy nói lại". Thử 3 lần không được: nhắc dùng nút chạm.

### 4.5 Cấu hình
Mọi ngưỡng ở trên đặt trong `utils/FeedbackConfig.kt` để hiệu chỉnh khi thử nghiệm. Ghi lại giá trị cuối cùng vào mục 9.

## 5. Kiến trúc & thay đổi code
| File | Việc |
|---|---|
| `tflite/RoboflowAnalyzer.kt`, màn Currency | **Xóa** |
| `ExploreScreen`, `HistoryScreen`, `ThemesScreen`, `HistoryManager` | Xóa hoặc gỡ khỏi navigation |
| `MainScreen.kt` | Chỉ còn Home / OCR / Find / Settings |
| `utils/FeedbackConfig.kt` | **Mới**: toàn bộ ngưỡng |
| `utils/FeedbackCoordinator.kt` | **Mới**: hàng đợi ưu tiên, cooldown, giới hạn |
| `utils/ImageQuality.kt` | **Mới**: Laplacian, vị trí và kích thước khung chữ |
| `utils/MotionMonitor.kt` | **Mới**: gyroscope |
| `utils/ReadingController.kt` | **Mới**: tách câu, dừng / tiếp / đọc lại |
| `ui/screens/OCRScreen.kt` | Gắn 4.2 + ReadingController |
| `ui/screens/FindScreen.kt` | Viết lại theo máy trạng thái 4.3; sửa lỗi 2 phiên nghe chồng nhau |
| `tflite/YoloAnalyzer.kt` | Bỏ đọc TTS trực tiếp (chuyển qua Coordinator); dùng `inputSize` thay cho 416 |
| `utils/HapticManager.kt` | Thêm các mẫu rung: ngắn / dài / đôi / lặp theo chu kỳ |
| `utils/MetricLogger.kt` | **Mới**: log `VIA_METRIC` + xuất CSV |
| `utils/SettingsManager.kt` | Thêm cờ `baselineMode` cho thử nghiệm A/B |

## 6. Đánh giá

### 6.1 Kiểm thử chức năng
Checklist cho từng luật ở mục 4, ví dụ: che bớt sáng → nghe "Ánh sáng yếu" trong ≤ 1 s. Ghi kết quả đạt / không đạt.

### 6.2 Đánh giá kỹ thuật
| Chỉ số | Dữ liệu / cách đo | Mục tiêu tham khảo |
|---|---|---|
| Precision / Recall / F1 nhận diện | 150 ảnh tự chụp (6 lớp × ~25), 2 mức sáng | P ≥ 0.8 |
| False positive / lượt | 20 lượt tìm có vật gây nhiễu; so sánh **có / không** luật 3 frame | Giảm ≥ 50% |
| CER / WER OCR | 30 mẫu (hộp thuốc, hóa đơn, sách) × 3 mức sáng; so với bản gõ tay | CER ≤ 10% khi đủ sáng |
| Tỷ lệ tự chụp thành công | Số lần chụp ra văn bản đúng / tổng lần chụp | ≥ 80% |
| Latency suy luận | Thời gian chạy YOLO / OCR mỗi frame (log) | ≤ 200 ms |
| Latency phản hồi | frame → bắt đầu rung / TTS | ≤ 500 ms |
| Mức quá tải | Số câu nói + số lần rung / phút trong mỗi lượt | So sánh A/B |

### 6.3 Thử nghiệm người dùng
- **6–8 người.** Ưu tiên người khiếm thị thật; nếu không đủ, dùng người sáng mắt bịt mắt và ghi rõ hạn chế.
- **Nhiệm vụ:** S1, S2, S3, S4 (mục 2), mỗi nhiệm vụ tối đa 120 s.
- **A/B (within-subject):** **A** = baseline VisionAid (`baselineMode`: rung theo bbox, đọc liên tục, không nhắc căn chỉnh) · **B** = VIAccess. Đổi thứ tự A/B giữa các người tham gia.
- **Đo:** tỷ lệ hoàn thành, thời gian, số lỗi (nói lệnh sai, chạm sai), tỷ lệ tìm đúng vật, SUS, NASA-TLX, phỏng vấn ngắn 3 câu.
- **Đạo đức:** phiếu đồng ý; không lưu ảnh hoặc giọng nói của người tham gia; chỉ lưu log ẩn danh.
- **Lưu dữ liệu:** `eval/` (CSV log, bảng kết quả, script tính chỉ số).

## 7. Phân công
Khối lượng tính theo điểm: **S = 1** (≤ nửa ngày) · **M = 2** (1–2 ngày) · **L = 3** (3+ ngày). Mỗi người **19 điểm**, gồm cả code, đánh giá và báo cáo.

| Người | Mảng chính (theo đề xuất) | Code | Đánh giá | Báo cáo / nộp | Tổng |
|---|---|---|---|---|---|
| **Tân** | Luồng chính, điều hướng, giọng nói, tích hợp | Dọn repo, Coordinator, lệnh giọng nói, xử lý lỗi | Checklist kiểm thử (chủ trì) | Chương kiến trúc & luồng; demo + video | 19 |
| **Tiến** | Đọc văn bản (T1) | ImageQuality, MotionMonitor, luật căn camera, OCRScreen | 30 mẫu OCR, CER/WER | Chương T1 + kết quả OCR; README | 19 |
| **Hùng** | Tìm vật thể (T2) | Máy trạng thái Find, mẫu rung, hướng, MetricLogger | 150 ảnh P/R/F1, FP, latency | Chương T2 + kết quả detection | 19 |
| **Hoàng** | TTS, accessibility, đánh giá người dùng | ReadingController, accessibility, Settings, onboarding | Thiết kế thử nghiệm, pilot, SUS/TLX, Wilcoxon | Chương HCI + khảo sát + kết quả người dùng; slide | 19 |

**Cả nhóm:** heuristic evaluation, thử nghiệm 6–8 người (mỗi người điều phối 2 người tham gia), đọc chéo code của nhau mỗi tuần, đọc soát báo cáo.

## 8. Tiến độ (ước lượng 8 tuần, điền ngày cụ thể)
| Tuần | Mốc | Việc |
|---|---|---|
| 1 | M0 – Repo sạch | Dọn dẹp (9.1), build trên máy thật, FeedbackConfig |
| 2–3 | M1 – T1 chạy end-to-end | Căn camera + auto-capture + dừng / tiếp / đọc lại |
| 2–3 | M1 – T2 chạy end-to-end | Máy trạng thái Find + rung + hướng |
| 4 | M2 – Tích hợp | Coordinator, giọng nói, accessibility, MetricLogger, baselineMode |
| 5 | M3 – Đánh giá kỹ thuật | Bộ ảnh / mẫu, P/R, CER, FP, latency; hiệu chỉnh ngưỡng |
| 6 | M4 – Thử nghiệm người dùng | Pilot 1 người → sửa → 6–8 người |
| 7 | M5 – Phân tích | Bảng số liệu, so sánh A/B, sửa lỗi |
| 8 | M6 – Nộp | Báo cáo, slide, video demo, hướng dẫn cài đặt |

Tuần 1 thêm: gửi cô duyệt phạm vi, khảo sát, wireframe (9.0). Tuần 5 thêm: heuristic evaluation.

**Đóng băng tính năng sau tuần 4.** Sau mốc này chỉ sửa lỗi và hiệu chỉnh ngưỡng.

## 9. Việc cần làm
Ký hiệu: `[S]` 1 điểm · `[M]` 2 điểm · `[L]` 3 điểm.

### 9.1 Tân — luồng chính & tích hợp (20)
- [x] Copy repo, bỏ dữ liệu / build, sửa `settings.gradle`
- [ ] `[S]` Gửi cô bản phạm vi đã thu gọn (mục 1, 4, 6) để duyệt
- [x] `[M]` `git init`, đẩy lên repo nhóm; mỗi người một nhánh (`tan`, `tien`, `hung`, `hoang`), gộp vào `main` qua Pull Request
- [x] Xóa `RoboflowAnalyzer.kt` (chứa API key)
- [x] `[M]` Gỡ Explore / History / Themes khỏi navigation
- [ ] `[S]` Build + chạy trên máy thật, ghi model máy test *(build OK; chưa thử máy thật)*
- [x] `[S]` Tạo `FeedbackConfig.kt`
- [x] `[L]` `FeedbackCoordinator` (hàng đợi ưu tiên, cooldown, giới hạn) + chuyển mọi lời nói qua Coordinator *(Find, OCR, YOLO, MainScreen; Setup/Tutorial/Settings/AccessibleButton còn gọi trực tiếp — Hoàng chuyển khi làm onboarding/Settings)*
- [ ] `[M]` 5 lệnh giọng nói + nút chạm dự phòng
- [ ] `[M]` Xử lý lỗi bằng giọng nói (thiếu quyền, thiếu giọng TTS tiếng Việt, mất mạng, lỗi mô hình) + dừng TTS / rung / camera khi app chạy nền
- [ ] `[S]` Chủ trì checklist kiểm thử chức năng (6.1)
- [ ] `[M]` Báo cáo: kiến trúc, luồng tương tác, bảng kế thừa / tự phát triển (1.4)
- [ ] `[M]` Chuẩn bị demo (máy, đạo cụ) + quay video demo S1–S4

### 9.2 Tiến — Đọc văn bản T1 (19)
- [ ] `[M]` Bỏ tiếng Anh: strings, `coco.txt`, nhánh `useVietnamese`
- [ ] `[L]` `ImageQuality`: Laplacian + vị trí / kích thước khung chữ
- [ ] `[M]` `MotionMonitor`: gyroscope
- [ ] `[L]` Luật căn camera 4.2 theo thứ tự ưu tiên + hiệu chỉnh ngưỡng
- [ ] `[M]` Tích hợp vào `OCRScreen` + auto-capture
- [ ] `[S]` `baselineMode` cho T1 (tắt nhắc căn chỉnh, đọc liên tục)
- [ ] `[L]` 30 mẫu OCR × 3 mức sáng + bản gõ tay, tính CER/WER, tỷ lệ tự chụp thành công
- [ ] `[M]` Báo cáo: chương T1 + kết quả OCR
- [ ] `[S]` README: hướng dẫn cài đặt và sử dụng

### 9.3 Hùng — Tìm vật thể T2 (19)
- [ ] `[M]` Danh sách 6 vật + lệnh "tìm <vật>"
- [ ] `[L]` Máy trạng thái SEARCHING / FOUND / TRACKING / CENTERED / LOST
- [ ] `[M]` Haptic patterns (ngắn / dài / đôi / lặp theo chu kỳ) + `baselineMode` cho T2
- [ ] `[S]` Hướng trái / giữa / phải
- [ ] `[S]` Unit test máy trạng thái Find + Coordinator
- [ ] `[M]` `MetricLogger` (`VIA_METRIC`) + xuất CSV
- [ ] `[L]` Bộ 150 ảnh vật thể + nhãn, tính P/R/F1
- [ ] `[M]` Đo false positive (có / không luật 3 frame) + đo latency từ log
- [ ] `[M]` Báo cáo: chương T2 + kết quả detection, FP, latency
- [ ] `[S]` Đóng gói dữ liệu mẫu test (ảnh, mẫu OCR, CSV log) để nộp

### 9.4 Hoàng — TTS, accessibility, đánh giá người dùng (20)
- [ ] `[M]` Khảo sát giải pháp liên quan (Seeing AI, Lookout, Envision, Be My Eyes, VisionAid)
- [ ] `[M]` Wireframe 3 màn + kịch bản âm thanh (mọi câu nói, mẫu rung)
- [ ] `[S]` Rút gọn onboarding + xin quyền
- [ ] `[M]` `ReadingController`: tách câu, dừng / tiếp / đọc lại
- [ ] `[M]` Accessibility: contentDescription, chữ ≥ 24sp, vùng chạm ≥ 48dp, tương phản ≥ 7:1
- [ ] `[M]` Settings: tốc độ đọc, rung, flash, mức chi tiết phản hồi
- [ ] `[M]` Kịch bản thử nghiệm, phiếu đồng ý, SUS / NASA-TLX
- [ ] `[S]` Pilot 1 người, chỉnh kịch bản
- [ ] `[M]` Script `eval/` tổng hợp chỉ số + Wilcoxon A/B
- [ ] `[M]` Báo cáo: chương HCI (persona, khảo sát, thiết kế) + kết quả người dùng
- [ ] `[S]` Slide thuyết trình

### 9.5 Cả nhóm
- [ ] Heuristic evaluation nội bộ (Nielsen) — tuần 5
- [ ] Thử nghiệm 6–8 người + A/B — mỗi người điều phối 2 người tham gia
- [ ] Đọc chéo code của nhau mỗi tuần
- [ ] Đọc soát báo cáo + tập thuyết trình

### 9.6 Làm nếu còn thời gian
- Đổi package / tên app / icon · theo dõi pin và nhiệt khi chạy lâu · unit test cho luật căn camera

## 10. Rủi ro
| Rủi ro | Ảnh hưởng | Giảm thiểu |
|---|---|---|
| OCR sai dấu tiếng Việt | CER cao | Đo và báo cáo trung thực; tập trung vào luật căn camera |
| Không tìm được người khiếm thị | Tính đại diện thấp | Người bịt mắt + 1–2 người thật nếu liên hệ được; ghi hạn chế |
| SpeechRecognizer cần mạng / nghe nhầm | Lệnh lỗi | Nút chạm lớn; buffer sau TTS; đếm lỗi nhận lệnh |
| Máy test yếu → latency cao | Rung trễ | Xử lý 1/N frame; dùng GPU delegate; ghi rõ model máy |
| Ngưỡng không hợp mọi môi trường | Nhắc sai | Hiệu chỉnh ở tuần 5, ghi giá trị cuối |
| Trễ tiến độ | Thiếu đánh giá | Đóng băng tính năng sau tuần 4 |

## 11. Quyết định đã chốt
- Bỏ "doors & windows": COCO không có lớp cửa / cửa sổ.
- Không tìm chìa khóa / ví: không có trong COCO, không huấn luyện mô hình mới.
- Bỏ Roboflow / Currency: cần mạng + API key, trái nguyên tắc xử lý trên thiết bị.
- Bỏ module `:tensorflow-lite` khỏi `settings.gradle`: thư mục không tồn tại (TFLite lấy từ Maven).
- Không dùng số liệu đánh giá của Nhóm 2 (là dữ liệu giả lập).
- Build: Gradle 8.5 + AGP 8.2.2 + JDK 21 (bản JDK đi kèm Android Studio). Bỏ `org.gradle.java.home` viết cứng.
- Trang trong MainScreen: 0 = Tìm vật, 1 = Đọc chữ, 2 = Cài đặt. Bỏ lệnh giờ / pin / trạng thái, giữ "giúp đỡ".
- Cách dùng Coordinator: `feedback.say(text, Priority.X, key = ..., queue = ..., userInitiated = ...)` và `feedback.vibrate(pattern, force)`. Lời do người dùng yêu cầu thì `userInitiated = true`; rung xác nhận thì `force = true`.

## 12. Nhật ký
| Ngày | Ai | Ghi chú |
|---|---|---|
| 2026-10-19 | — | Tạo repo VIAccess từ VisionAid, viết CLAUDE.md / PLAN.md |
| 2026-10-22 | Tân | Nhánh tan/foundation: sửa build, gỡ Explore/History/Themes, FeedbackConfig, FeedbackCoordinator + 8 unit test |

# Hướng dẫn cho thành viên

## Cài đặt (một lần)
1. Cài **Android Studio** (có sẵn SDK + JDK) và **Claude Code**.
2. Clone repo:
   ```
   git clone https://github.com/nguyentanjr/VIAccess.git
   cd VIAccess
   ```
3. Mở thư mục bằng Android Studio một lần để Gradle tải thư viện. Bật USB debugging trên điện thoại.

## Mỗi lần làm việc
1. Cập nhật code: `git pull --rebase`
2. Mở Claude Code trong thư mục `VIAccess`, gõ:
   > Mình là **<Tên>**, làm việc **<tên việc>** trong mục 9 của PLAN.md.
3. Cắm máy thật, thử theo hướng dẫn Claude đưa ra.
4. Xong: build được → tick checkbox trong `PLAN.md` → commit → `git pull --rebase` → `git push`.

## Quy tắc
- Chỉ làm việc của mình (PLAN.md mục 9). Không tự thêm tính năng ngoài phạm vi.
- Commit nhỏ, build được mới push. Không dùng `git push --force`.
- Vướng mắc hoặc muốn đổi quyết định: ghi vào PLAN.md mục 11 và báo nhóm.

## Thứ tự
Tuần 1, Tân làm phần nền (`FeedbackConfig`, `FeedbackCoordinator`). Trong lúc đó:
- **Hoàng:** khảo sát + wireframe
- **Tiến:** chụp 30 mẫu OCR
- **Hùng:** chụp 150 ảnh vật thể

Sau khi phần nền vào `main`, mọi người bắt đầu code.

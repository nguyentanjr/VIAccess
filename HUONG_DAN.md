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
Mỗi người một nhánh: **`tan`**, **`tien`**, **`hung`**, **`hoang`**.
1. Vào nhánh của mình và lấy code mới của nhóm:
   ```
   git checkout <nhánh>
   git pull
   git merge origin/main
   ```
2. Mở Claude Code trong thư mục `VIAccess`, gõ:
   > Mình là **<Tên>**, làm việc **<tên việc>** trong mục 9 của PLAN.md.
3. Cắm máy thật, thử theo hướng dẫn Claude đưa ra.
4. Xong một việc: build được → tick checkbox trong `PLAN.md` → commit → `git push`.
5. Lên GitHub mở **Pull Request** từ nhánh của mình vào `main`. Một bạn khác xem rồi bấm Merge.

## Quy tắc
- Chỉ làm việc của mình (PLAN.md mục 9). Không tự thêm tính năng ngoài phạm vi.
- Chỉ commit vào nhánh của mình, không commit thẳng vào `main`. Không dùng `git push --force`.
- Vướng mắc hoặc muốn đổi quyết định: ghi vào PLAN.md mục 11 và báo nhóm.

## Thứ tự
Tuần 1, Tân làm phần nền (`FeedbackConfig`, `FeedbackCoordinator`). Trong lúc đó:
- **Hoàng:** khảo sát + wireframe
- **Tiến:** chụp 30 mẫu OCR
- **Hùng:** chụp 150 ảnh vật thể

Sau khi phần nền vào `main`, mọi người bắt đầu code.

# SOFTWARE REQUIREMENT SPECIFICATION (SRS) - PEER GRADING SYSTEM

Tài liệu Đặc tả Yêu cầu Phần mềm (SRS) cho tính năng Chấm bài chéo (Peer Review) thuộc hệ thống E-Learning.

---

## 1. Thiết Kế Entity & Mối Quan Hệ (Entity Relationship Model)

Dựa trên hai Entity có sẵn là `User` và `Course`, hệ thống được mở rộng thêm các thực thể: `Assignment`, `Submission` và `PeerReview`.

### 1.1. Sơ đồ quan hệ tổng quan (ERD)

```
User [role: INSTRUCTOR] ──(1)───instructor───(1)── Course
User [role: STUDENT]    ──(n)───students─────(n)── Course     (bảng trung gian course_students)

Course (1) ───< Assignment (1) ───< Submission >─── (1) User [student / owner]
                                          │
                                          └───< PeerReview >─── (1) User [reviewer]
```

### 1.2. Mô tả chi tiết từng Entity

- **User (Người dùng)** — *đã có sẵn*
  - Có vai trò (`role`) là `STUDENT` hoặc `INSTRUCTOR`.
  - Một `Course` có một `instructor` (User).
  - Một `Course` có nhiều học viên (`students` — danh sách `User` thông qua quan hệ `@ManyToMany`).
  - Một học viên (`student`) có thể thực hiện nhiều `Submission`.
  - Một học viên có thể tham gia nhiều lượt chấm chéo (`PeerReview`) với vai trò là `reviewer`.

- **Course (Khóa học)** — *đã có sẵn, bổ sung quan hệ*
  - Chứa thông tin về khóa học, giảng viên phụ trách (`instructor`).
  - Liên kết với danh sách học viên tham gia (`students`) qua bảng trung gian `course_students`.
  - Có nhiều bài tập (`Assignment`).

- **Assignment (Bài tập)** — *mới*
  - Thuộc về một `Course` cụ thể (`ManyToOne`).
  - Có tiêu đề, mô tả và hạn nộp (`dueDate`).
  - Có nhiều bài nộp (`Submission`).

- **Submission (Bài nộp)** — *mới*
  - Do một học viên (`student`, đóng vai trò Owner của bài nộp) nộp (`ManyToOne`).
  - Thuộc về một bài tập (`Assignment`) cụ thể (`ManyToOne`).
  - Chứa nội dung làm bài (`content`), điểm số chính thức (`officialGrade`), và thời gian nộp (`submittedAt`).
  - Có trạng thái bài nộp (`status`): `PENDING` (đang chờ chấm) hoặc `COMPLETED` (đã chấm xong).
  - Liên kết tới đúng 2 lượt chấm chéo (`PeerReview`).

- **PeerReview (Lượt chấm chéo)** — *mới*
  - Thuộc về một bài nộp (`Submission`) cần chấm (`ManyToOne`).
  - Được giao cho một học viên đóng vai trò người chấm (`reviewer`) (`ManyToOne`).
  - Lưu trữ điểm số độc lập do người chấm này nhập (`reviewerGrade`) và thời gian chấm (`reviewedAt`).

> **Ghi chú đặt tên:** để tránh nhầm lẫn giữa 2 cấp điểm số, `Submission` dùng field `officialGrade` (điểm chính thức, là trung bình cộng), còn `PeerReview` dùng field `reviewerGrade` (điểm độc lập do từng người chấm nhập). Hai field này **không dùng chung tên `grade`**.

---

## 2. Quy Tắc Phân Công Ngẫu Nhiên (Random Reviewer Assignment Rules)

Khi một học viên nộp bài tập (tạo một `Submission` mới), hệ thống thực hiện tuần tự các bước sau **trong cùng một giao dịch (transaction)**:

1. **Kiểm tra điều kiện biên**: Đếm số học viên khác (không tính người nộp) đã ghi danh trong `Course` chứa `Assignment` đó.
   - Điều kiện phải thỏa: `(tổng số học viên ghi danh trong Course) - 1 (người nộp) ≥ 2`, tức tổng số học viên ghi danh trong Course phải **≥ 3**.
   - Nếu không đủ, hệ thống chặn hành động nộp bài và trả về lỗi: `"Không đủ người chấm trong khóa học"` (HTTP 400 Bad Request). `Submission` sẽ **không được tạo** trong trường hợp này.
2. **Tạo Submission**: Nếu đủ điều kiện, tạo bản ghi `Submission` với `status = PENDING`, `officialGrade = null`.
3. **Chỉ định Reviewer**: Ngay lập tức, trong cùng giao dịch, hệ thống:
   - Lấy danh sách toàn bộ học viên (`role = STUDENT`) đã ghi danh trong `Course`.
   - Loại trừ chính học viên nộp bài (`student_id` của `Submission`) khỏi danh sách ứng viên.
   - **Tráo ngẫu nhiên** (thuật toán Shuffle) danh sách ứng viên còn lại, đảm bảo tính khách quan thay vì chọn theo thứ tự tuần tự.
   - Chọn ra chính xác **2 người** đầu tiên sau khi tráo, tạo 2 bản ghi `PeerReview` tương ứng với `reviewerGrade = null`, `reviewedAt = null`, mỗi bản ghi liên kết tới `Submission` vừa tạo và 1 trong 2 reviewer.
4. **Ràng buộc bất biến**: Tại mọi thời điểm, mỗi `Submission` có đúng 2 `PeerReview` liên kết, với `reviewer_id` khác nhau và khác `student_id` (Owner) của `Submission` đó.

---

## 3. Quy Tắc Tính Điểm & Trạng Thái (Grading & Status Lifecycle)

Quy trình cập nhật điểm số và trạng thái của bài nộp tuân theo vòng đời sau:

1. **Trạng thái ban đầu**: Ngay sau Bước 2-3 ở Mục 2, `Submission` ở trạng thái **`PENDING`** với `officialGrade = null`, kèm 2 `PeerReview` chờ chấm (`reviewerGrade = null`).

2. **Quá trình chấm điểm độc lập**: Hai người chấm chéo được phân công thực hiện chấm điểm độc lập qua API chấm điểm. Mỗi lần gọi API:
   - Hệ thống xác thực người gọi API (`reviewerEmail`/`reviewerId`) đúng là `reviewer` được gán cho `PeerReview` đó — nếu sai, trả lỗi `"Người chấm không hợp lệ"` (HTTP 403 Forbidden).
   - Ghi nhận `reviewerGrade` và `reviewedAt` vào đúng bản ghi `PeerReview`.
   - Nếu đây là lượt chấm đầu tiên của Submission (mới 1/2 `PeerReview` có điểm), `Submission` **vẫn giữ nguyên trạng thái `PENDING`**.

3. **Cập nhật điểm chính thức và chuyển trạng thái**: Sau mỗi lần chấm, hệ thống kiểm tra: nếu **cả 2** `PeerReview` của `Submission` đó đã có `reviewerGrade` khác `null`, hệ thống thực hiện đồng thời:
   - Tính điểm trung bình cộng:

     Grade_official = (Grade_reviewer1 + Grade_reviewer2) / 2

   - Cập nhật `officialGrade` của `Submission` bằng giá trị vừa tính.
   - Chuyển trạng thái `Submission` từ `PENDING` sang **`COMPLETED`**.

4. **Bất biến sau khi hoàn tất**: Một khi `Submission` đã ở trạng thái `COMPLETED`, `officialGrade` không được tính lại hoặc ghi đè bởi các thao tác chấm điểm khác (vì mỗi `Submission` chỉ có đúng 2 `PeerReview`, không có lượt chấm thứ 3).

---

## 4. Tóm tắt Exception nghiệp vụ liên quan

| Tình huống | HTTP Status | Thông điệp |
|---|---|---|
| Không đủ học viên khác trong Course để phân công (< 2 ứng viên) | 400 Bad Request | "Không đủ người chấm trong khóa học" |
| Người gọi API chấm điểm không phải reviewer được phân công / tự chấm bài mình | 403 Forbidden | "Người chấm không hợp lệ" |
| Không tìm thấy Course/Assignment/Submission/PeerReview theo id | 404 Not Found | "Không tìm thấy [resource] với id = ..." |
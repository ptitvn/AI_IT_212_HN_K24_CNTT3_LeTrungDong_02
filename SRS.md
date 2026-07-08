# SOFTWARE REQUIREMENT SPECIFICATION (SRS) - PEER GRADING SYSTEM

Tài liệu Đặc tả Yêu cầu Phần mềm (SRS) cho tính năng Chấm bài chéo (Peer Review) thuộc hệ thống E-Learning.

---

## 1. Thiết Kế Entity & Mối Quan Hệ (Entity Relationship Model)

Dựa trên hai Entity có sẵn là `User` và `Course`, hệ thống được mở rộng thêm các thực thể: `Assignment`, `Submission` và `PeerReview`.

### Sơ đồ mối quan hệ (Entity Relationships)
- **User (Người dùng)**:
  - Có vai trò (`role`) là `STUDENT` hoặc `INSTRUCTOR`.
  - Một `Course` có một `instructor` (User).
  - Một `Course` có nhiều học viên (`students` - danh sách `User` thông qua quan hệ `@ManyToMany`).
  - Một học viên (`student`) có thể thực hiện nhiều `Submission`.
  - Một học viên có thể tham gia nhiều lượt chấm chéo (`PeerReview`) với vai trò là `reviewer`.

- **Course (Khóa học)**:
  - Chứa thông tin về khóa học, giảng viên phụ trách (`instructor`).
  - Liên kết với danh sách học viên tham gia (`students`) qua bảng trung gian `course_students`.
  - Có nhiều bài tập (`Assignment`).

- **Assignment (Bài tập)**:
  - Thuộc về một `Course` cụ thể (`ManyToOne`).
  - Có tiêu đề, mô tả và hạn nộp (`dueDate`).
  - Có nhiều bài nộp (`Submission`).

- **Submission (Bài nộp)**:
  - Do một học viên (`student`) nộp (`ManyToOne`).
  - Thuộc về một bài tập (`Assignment`) cụ thể (`ManyToOne`).
  - Chứa nội dung làm bài (`content`), điểm số chính thức (`grade`), và thời gian nộp (`submittedAt`).
  - Có trạng thái bài nộp (`status`): `PENDING` (đang chờ chấm) hoặc `COMPLETED` (đã chấm xong).
  - Liên kết tới các lượt chấm chéo (`PeerReview`).

- **PeerReview (Lượt chấm chéo)**:
  - Thuộc về một bài nộp (`Submission`) cần chấm (`ManyToOne`).
  - Được giao cho một học viên đóng vai trò người chấm (`reviewer`) (`ManyToOne`).
  - Lưu trữ điểm số do người chấm này nhập (`grade`) và thời gian chấm (`reviewedAt`).

---

## 2. Quy Tắc Phân Công Ngẫu Nhiên (Random Reviewer Assignment Rules)

Khi một học viên nộp bài tập (tạo một `Submission` mới):
1. **Chỉ định Reviewer**: Hệ thống tự động phân công chính xác **2 người chấm chéo (Reviewer)** cho mỗi `Submission`.
2. **Đối tượng được chấm**: Reviewer bắt buộc phải là những học viên (`User` có vai trò `STUDENT`) đã tham gia (được enroll) cùng khóa học đó.
3. **Quy tắc loại trừ**: Reviewer **KHÔNG** được phép là chính học viên nộp bài đó (`student_id` của `Submission` khác `reviewer_id` của `PeerReview`).
4. **Tính ngẫu nhiên (Randomization)**: Danh sách các học viên hợp lệ sẽ được tráo ngẫu nhiên (sử dụng thuật toán Shuffle) để lựa chọn ra 2 người chấm chéo, đảm bảo tính khách quan thay vì chọn theo thứ tự tuần tự.
5. **Điều kiện biên**: Nếu khóa học không có đủ số lượng học viên tối thiểu (phải có ít nhất 2 học viên khác học viên nộp bài, tức tổng số học viên trong khóa học $\ge 3$), hệ thống sẽ chặn hành động nộp bài và báo lỗi: `"Không đủ người chấm trong khóa học"` (HTTP 400).

---

## 3. Quy Tắc Tính Điểm & Trạng Thái (Grading & Status Lifecycle)

Quy trình cập nhật điểm số và trạng thái của bài nộp tuân theo vòng đời sau:
1. **Trạng thái ban đầu**:
   - Khi bài nộp được tạo lập thành công, trạng thái ban đầu của `Submission` luôn là **`PENDING`**.
   - Trường điểm số chính thức (`grade`) của `Submission` ban đầu là `null`.
2. **Quá trình chấm điểm độc lập**:
   - Hai người chấm chéo được phân công sẽ thực hiện chấm điểm độc lập qua API chấm điểm.
   - Điểm số của từng người chấm được ghi nhận vào bản ghi `PeerReview` tương ứng.
3. **Cập nhật điểm chính thức và Trạng thái**:
   - Điểm trung bình cộng chính thức của bài tập chỉ được tính và cập nhật vào `Submission` **khi và chỉ khi cả 2 người chấm chéo** đã hoàn thành việc nhập điểm (`grade` của cả 2 `PeerReview` khác `null`).
   - Khi điều kiện trên thỏa mãn, hệ thống sẽ thực hiện:
     - Tính điểm trung bình cộng: $\text{Grade}_{\text{official}} = \frac{\text{Grade}_{\text{Reviewer 1}} + \text{Grade}_{\text{Reviewer 2}}}{2}$.
     - Cập nhật trường `grade` của `Submission`.
     - Chuyển trạng thái `Submission` từ `PENDING` sang **`COMPLETED`**.

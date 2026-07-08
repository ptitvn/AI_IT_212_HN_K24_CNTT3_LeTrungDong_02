# LỊCH SỬ CÂU LỆNH TƯƠNG TÁC AI (PROMPT HISTORY)

Tài liệu này ghi lại các câu lệnh (prompts) chi tiết và có cấu trúc được sử dụng trong quá trình làm bài thi để hướng dẫn AI hoàn thành các nhiệm vụ của **Đề 02 - Hệ thống E-Learning Chấm bài chéo**.

---

## Bước 1: Phân tích & Đặc tả Yêu cầu (Nhiệm vụ 1)

**Prompt:**
> "Tôi đang làm một dự án E-Learning trên Spring Boot với hai thực thể có sẵn là User và Course. Giờ tôi cần phát triển tính năng 'Chấm bài chéo' (Peer Review).
> 
> Hãy viết file tài liệu đặc tả yêu cầu `SRS.md` và lưu vào thư mục gốc của dự án. File đặc tả phải bao gồm:
> 1. Thiết kế các thực thể cần bổ sung:
>    - `Assignment` (Bài tập): Thuộc về một Course.
>    - `Submission` (Bài nộp): Thuộc về một Assignment, do một Student (User) nộp, có chứa nội dung bài làm, điểm chính thức (grade), trạng thái (status: PENDING/COMPLETED) và ngày nộp.
>    - `PeerReview` (Lượt chấm chéo): Thuộc về một Submission, được chấm bởi một Reviewer (User), lưu điểm số độc lập.
> 2. Quy tắc phân công ngẫu nhiên:
>    - Mỗi bài nộp (Submission) phải được tự động gán chính xác cho 2 người chấm chéo (Reviewer).
>    - Reviewer phải là học viên trong cùng khóa học và tuyệt đối không được là người nộp bài tập đó.
>    - Lựa chọn Reviewer phải hoàn toàn ngẫu nhiên (random).
> 3. Quy tắc tính điểm và cập nhật trạng thái:
>    - Khi mới nộp, trạng thái bài nộp mặc định là `PENDING` và điểm chính thức là null.
>    - Chỉ khi cả 2 người chấm chéo đều hoàn thành việc nhập điểm độc lập, hệ thống mới tính điểm trung bình cộng của 2 người chấm làm điểm chính thức cho Submission và chuyển trạng thái bài nộp sang `COMPLETED`."

---

## Bước 2: Thiết kế Entities, Repositories và DTOs (Nhiệm vụ 2 - Phần 1)

**Prompt:**
> "Dựa trên tài liệu đặc tả `SRS.md` đã thiết kế:
> 
> 1. Hãy cập nhật thực thể `Course.java` để thêm danh sách học viên `@ManyToMany List<User> students` tham gia khóa học.
> 2. Tạo mới các thực thể JPA tương ứng với các thuộc tính và quan hệ như mô tả:
>    - `Assignment.java`
>    - `Submission.java`
>    - `PeerReview.java`
> 3. Định nghĩa các interface JPA Repositories tương ứng cho các thực thể mới trong package `com.elearning.models.repositories`:
>    - `AssignmentRepository`
>    - `SubmissionRepository`
>    - `PeerReviewRepository`
> 4. Tạo các DTO (Data Transfer Objects) cần thiết trong package `com.elearning.models.dto` để nhận dữ liệu từ yêu cầu client gửi lên:
>    - `CreateAssignmentRequest` (để tạo bài tập)
>    - `SubmitAssignmentRequest` (để nộp bài làm)
>    - `GradeReviewRequest` (để nhập điểm chấm chéo)"

---

## Bước 3: Cài đặt Logic Dịch vụ và REST Controllers (Nhiệm vụ 2 - Phần 2)

**Prompt:**
> "Bây giờ hãy viết lớp nghiệp vụ `ElearningService.java` để xử lý các luồng nghiệp vụ sau:
> 
> 1. `enrollStudent(Long courseId, String email)`: Đăng ký một học viên vào khóa học.
> 2. `createAssignment(CreateAssignmentRequest request)`: Tạo bài tập mới.
> 3. `submitAssignment(SubmitAssignmentRequest request, String studentEmail)`:
>    - Kiểm tra học viên nộp bài có tham gia khóa học đó không.
>    - Kiểm tra nếu khóa học có ít hơn 2 học viên khác, ném ra ngoại lệ lỗi nghiệp vụ.
>    - Tráo ngẫu nhiên (shuffle) danh sách học viên cùng khóa học để chọn ra 2 người chấm chéo không trùng với người nộp.
>    - Tạo mới Submission với trạng thái ban đầu là `PENDING`.
>    - Tạo 2 bản ghi PeerReview được gán cho 2 người chấm ngẫu nhiên đã chọn.
> 4. `gradeReview(Long reviewId, GradeReviewRequest request, String reviewerEmail)`:
>    - Kiểm tra tính hợp lệ của người chấm (phải là người được gán chấm chéo và không được tự chấm bài của chính mình).
>    - Lưu điểm số do người chấm nhập.
>    - Kiểm tra xem cả 2 người chấm đã chấm điểm chưa. Nếu cả hai đã nhập điểm, tự động cập nhật điểm chính thức bằng trung bình cộng của 2 điểm chấm và đổi trạng thái Submission sang `COMPLETED`.
> 
> Đồng thời, hãy tạo `ElearningController.java` để cung cấp các API endpoints sau:
> - `POST /api/v1/courses/{courseId}/enroll`
> - `POST /api/v1/assignments`
> - `POST /api/v1/submissions`
> - `POST /api/v1/peer-reviews/{reviewId}/grade`"

---

## Bước 4: Xử lý Ngoại lệ và Ràng buộc Nghiệp vụ (Nhiệm vụ 3)

**Prompt:**
> "Chúng ta cần đảm bảo hệ thống kiểm soát tốt các lỗi nghiệp vụ và phản hồi lỗi chuyên nghiệp về phía Front-end.
> 
> Hãy cập nhật lớp `GlobalExceptionHandler.java` để xử lý lỗi nghiệp vụ `BusinessException.class` và các ngoại lệ khác. Đảm bảo:
> 1. Khi ném ra lỗi 'Không đủ người chấm trong khóa học' sẽ trả về mã trạng thái `HTTP 400 Bad Request` kèm thông điệp rõ ràng.
> 2. Khi người chấm không hợp lệ (không đúng người được phân công hoặc tự chấm bài của mình) sẽ trả về mã trạng thái `HTTP 403 Forbidden` kèm thông điệp rõ ràng.
> 3. Phản hồi trả về phải tuân thủ định dạng lớp API chung `ApiResponse` của hệ thống."

---

## Bước 5: Viết Kiểm thử Tự động (Verification)

**Prompt:**
> "Tôi muốn viết một bộ kiểm thử tích hợp (integration tests) để chạy thử toàn bộ luồng nghiệp vụ nhằm đảm bảo không có lỗi Stack-trace hay lỗi logic nào.
> 
> 1. Hãy cấu hình thêm cơ sở dữ liệu in-memory H2 trong file `build.gradle` làm dependency cho môi trường test và tạo file cấu hình `src/test/resources/application.properties` để chạy cơ sở dữ liệu H2 giả lập (giúp test chạy độc lập và nhanh chóng).
> 2. Cập nhật file test `ElearningBaseApplicationTests.java` để tự động:
>    - Tạo giảng viên và 3 học viên mẫu (A, B, C).
>    - Đăng ký cả 3 học viên vào khóa học.
>    - Học viên A nộp bài, kiểm tra xem hệ thống có gán chính xác B và C làm người chấm chéo không.
>    - Học viên B chấm điểm, kiểm tra bài nộp vẫn giữ trạng thái PENDING.
>    - Học viên C chấm điểm, kiểm tra bài nộp đã chuyển sang trạng thái COMPLETED với điểm trung bình chính xác.
>    - Kiểm thử các trường hợp ngoại lệ: không đủ người chấm (chỉ có 2 người trong lớp) và chấm điểm trái phép."

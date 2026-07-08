# LỊCH SỬ CÂU LỆNH TƯƠNG TÁC AI (PROMPT HISTORY)

Tài liệu này ghi lại các câu lệnh (prompts) chi tiết và có cấu trúc được sử dụng trong quá trình làm bài thi để hướng dẫn AI hoàn thành các nhiệm vụ của **Đề 02 - Hệ thống E-Learning Chấm bài chéo**.

---

## Bước 0: Cho AI đọc và nắm bắt Base Code hiện tại

Trước khi yêu cầu thiết kế bất kỳ tính năng gì, tôi cung cấp cho AI toàn bộ ngữ cảnh của dự án hiện có để đảm bảo mọi thiết kế sau này bám sát đúng cấu trúc thật, không đoán mò.

**Prompt:**
> "Đây là cấu trúc thư mục hiện tại của dự án Base Code (package gốc `com.elearning`):
> ```
> com.elearning
> ├── ElearningBaseApplication.java
> ├── models
> │   ├── entities/        (User.java, Course.java)
> │   ├── repositories/    (UserRepository.java, CourseRepository.java)
> │   └── dto/
> ├── security
> │   ├── jwt/JwtAuthenticationFilter.java
> │   └── principal/CustomUserDetailsService.java
> └── controllers/
> ```
> Đây là toàn bộ nội dung file `User.java`:
> ```java
> [dán nguyên văn nội dung User.java]
> ```
> Đây là toàn bộ nội dung file `Course.java`:
> ```java
> [dán nguyên văn nội dung Course.java]
> ```
> Hãy đọc kỹ và tóm tắt lại cho tôi: các field hiện có, các quan hệ (annotation JPA) đã tồn tại giữa `User` và `Course`, và cho biết liệu đã có sẵn cơ chế học viên ghi danh (enroll) vào khóa học hay chưa. Tôi sẽ dựa trên phân tích này để thiết kế tính năng Peer Review mà không phá vỡ cấu trúc cũ."

**Kết quả:** AI xác nhận `User` có các field `id, email, fullName, role...`, `Course` có `id, name, teacher...`, và **chưa có quan hệ** giữa `User` và `Course` để biết học viên nào ghi danh khóa nào → cần bổ sung ở bước tiếp theo.

---

## Bước 1: Phân tích & Đặc tả Yêu cầu (Nhiệm vụ 1)

**Prompt:**
> "Tôi đang làm một dự án E-Learning trên Spring Boot với hai thực thể có sẵn là `User` và `Course` (nội dung như đã cung cấp ở Bước 0). Giờ tôi cần phát triển tính năng 'Chấm bài chéo' (Peer Review) theo yêu cầu thực tế sau từ khách hàng:
> 
> *"Khóa học đông quá giảng viên chấm bài không xuể. Khi học viên nộp một bài tập (Submission), hệ thống sẽ tự động chỉ định ngẫu nhiên 2 học viên khác (trong cùng khóa học đó) để làm Người chấm bài (Reviewer). Hai người này sẽ nhập điểm độc lập. Điểm chính thức của bài tập đó sẽ là điểm trung bình cộng của 2 người chấm. Lưu ý tuyệt đối không được phân công học viên tự chấm bài của chính mình."*
> 
> Hãy viết file tài liệu đặc tả yêu cầu `SRS.md` và lưu vào thư mục gốc của dự án. File đặc tả phải bao gồm:
> 1. Thiết kế các thực thể cần bổ sung:
>    - `Assignment` (Bài tập): Thuộc về một Course.
>    - `Submission` (Bài nộp): Thuộc về một Assignment, do một Student (User) nộp, có chứa nội dung bài làm, điểm chính thức (grade), trạng thái (status: PENDING/COMPLETED) và ngày nộp.
>    - `PeerReview` (Lượt chấm chéo): Thuộc về một Submission, được chấm bởi một Reviewer (User), lưu điểm số độc lập.
> 2. Vì `Course` hiện chưa có quan hệ với `User`, hãy đề xuất bổ sung quan hệ ghi danh (enrollment) phù hợp và giải thích lý do chọn ManyToMany trực tiếp thay vì entity Enrollment riêng.
> 3. Quy tắc phân công ngẫu nhiên:
>    - Mỗi bài nộp (Submission) phải được tự động gán chính xác cho 2 người chấm chéo (Reviewer).
>    - Reviewer phải là học viên trong cùng khóa học và tuyệt đối không được là người nộp bài tập đó.
>    - Lựa chọn Reviewer phải hoàn toàn ngẫu nhiên (random), không xếp theo thứ tự.
> 4. Quy tắc tính điểm và cập nhật trạng thái:
>    - Khi mới nộp, trạng thái bài nộp mặc định là `PENDING` và điểm chính thức là null.
>    - Chỉ khi cả 2 người chấm chéo đều hoàn thành việc nhập điểm độc lập, hệ thống mới tính điểm trung bình cộng của 2 người chấm làm điểm chính thức cho Submission và chuyển trạng thái bài nộp sang `COMPLETED`."

---

## Bước 2: Thiết kế Entities, Repositories và DTOs (Nhiệm vụ 2 - Phần 1)

Ở bước này, tôi chủ động **chẻ nhỏ task theo từng file** thay vì yêu cầu AI sinh toàn bộ Entity/Repository/DTO cùng lúc, để có thể kiểm tra và xác nhận từng phần trước khi đi tiếp.

### 2.1. Bổ sung quan hệ cho `Course`

**Prompt:**
> "Dựa trên đề xuất ở SRS.md, hãy cập nhật thực thể `Course.java` (giữ nguyên toàn bộ field/quan hệ cũ) để thêm quan hệ `@ManyToMany` với `User` đại diện cho danh sách học viên đã ghi danh: `private Set<User> enrolledStudents`. Chỉ sửa đúng phần này, không đụng vào các field khác của Course."

*(Xác nhận kết quả: Course.java build được, không phá vỡ mapping cũ, trước khi sang bước tiếp theo.)*

### 2.2. Tạo `Assignment.java`

**Prompt:**
> "Tạo thực thể `Assignment.java` trong package `com.elearning.models.entities`, quan hệ `@ManyToOne` với `Course` đã có sẵn. Các field: `id, title, description, course, createdAt`."

*(Xác nhận: entity tạo bảng đúng, mapping course_id đúng.)*

### 2.3. Tạo `Submission.java` và `PeerReview.java`

**Prompt:**
> "Tạo tiếp 2 thực thể `Submission.java` và `PeerReview.java` theo đúng thiết kế trong `SRS.md`:
> - `Submission`: quan hệ ManyToOne với `Assignment` và với `User` (owner), có `status` (enum PENDING/COMPLETED), `averageScore`, `submittedAt`, và quan hệ OneToMany với `PeerReview`.
> - `PeerReview`: quan hệ ManyToOne với `Submission` và với `User` (reviewer), có `score`, `graded`, `assignedAt`, `gradedAt`.
> Vì 2 entity này liên quan chặt chẽ với nhau (PeerReview phụ thuộc trực tiếp vào Submission) nên tôi để AI tạo cùng lúc, nhưng yêu cầu giữ đúng convention (Lombok, `@PrePersist`) như 2 entity `Assignment`/`Course` đã tạo ở bước trước."

### 2.4. Tạo Repository và DTO

**Prompt:**
> "Định nghĩa các interface JPA Repository tương ứng cho 3 entity mới, đặt trong package `com.elearning.models.repositories` (đúng package đã dùng cho `UserRepository`, `CourseRepository` có sẵn):
> - `AssignmentRepository`
> - `SubmissionRepository`
> - `PeerReviewRepository`
> 
> Sau đó tạo các DTO trong package `com.elearning.models.dto`:
> - `CreateAssignmentRequest` (để tạo bài tập)
> - `SubmitAssignmentRequest` (để nộp bài làm)
> - `GradeReviewRequest` (để nhập điểm chấm chéo)"

---

## Bước 3: Cài đặt Logic Dịch vụ và REST Controllers (Nhiệm vụ 2 - Phần 2)

**Prompt:**
> "Bây giờ hãy viết lớp nghiệp vụ `ElearningService.java` để xử lý các luồng nghiệp vụ sau, sử dụng các Entity/Repository đã tạo ở Bước 2:
> 
> 1. `enrollStudent(Long courseId, String email)`: Đăng ký một học viên vào khóa học (dùng quan hệ `enrolledStudents` vừa thêm vào `Course`).
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
> Lưu ý: `studentEmail`/`reviewerEmail` sẽ lấy từ `Authentication` (JWT) có sẵn trong `CustomUserDetailsService`, không truyền userId thô từ client.
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

## Bước 5: Debug lỗi phát sinh khi chạy thử (Run Application)

Khi chạy `./gradlew bootRun` để kiểm tra Yêu cầu 1 của Nhiệm vụ 3, tôi gặp lỗi kết nối cơ sở dữ liệu và nhờ AI hỗ trợ đọc log để xác định nguyên nhân gốc.

**Prompt:**
> "Tôi chạy project và gặp log lỗi sau, hãy đọc và cho tôi biết nguyên nhân gốc là gì, đâu là lỗi thật sự cần sửa và đâu chỉ là hệ quả:
> ```
> com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure
> Caused by: java.net.ConnectException: Connection refused: getsockopt
> ...
> Unable to determine Dialect without JDBC metadata
> ...
> Error creating bean with name 'userRepository' ... Cannot resolve reference to bean 'jpaSharedEM_entityManagerFactory'
> ```"

**Kết quả:** AI xác định nguyên nhân gốc là MySQL chưa chạy ở cổng cấu hình trong `application.properties` (không phải lỗi logic code), toàn bộ các lỗi Bean/EntityManagerFactory phía sau chỉ là hệ quả domino. Sau khi khởi động lại MySQL đúng cổng, ứng dụng chạy thành công không còn Stack-trace nào khác.

---

## Bước 6: Viết Kiểm thử Tự động (Verification)

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
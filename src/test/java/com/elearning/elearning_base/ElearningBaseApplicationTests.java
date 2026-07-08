package com.elearning.elearning_base;

import com.elearning.exceptions.BusinessException;
import com.elearning.models.dto.*;
import com.elearning.models.entities.*;
import com.elearning.models.repositories.*;
import com.elearning.models.services.ElearningService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ElearningBaseApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private PeerReviewRepository peerReviewRepository;

    @Autowired
    private ElearningService elearningService;

    private User instructor;
    private User studentA;
    private User studentB;
    private User studentC;
    private Course course;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        // Clean database
        peerReviewRepository.deleteAll();
        submissionRepository.deleteAll();
        assignmentRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();

        // Create Instructor
        instructor = new User();
        instructor.setFullName("Instructor Jones");
        instructor.setEmail("instructor@elearning.com");
        instructor.setPassword("password123");
        instructor.setRole("INSTRUCTOR");
        instructor = userRepository.save(instructor);

        // Create Students
        studentA = new User();
        studentA.setFullName("Student A");
        studentA.setEmail("studenta@elearning.com");
        studentA.setPassword("password123");
        studentA.setRole("STUDENT");
        studentA = userRepository.save(studentA);

        studentB = new User();
        studentB.setFullName("Student B");
        studentB.setEmail("studentb@elearning.com");
        studentB.setPassword("password123");
        studentB.setRole("STUDENT");
        studentB = userRepository.save(studentB);

        studentC = new User();
        studentC.setFullName("Student C");
        studentC.setEmail("studentc@elearning.com");
        studentC.setPassword("password123");
        studentC.setRole("STUDENT");
        studentC = userRepository.save(studentC);

        // Create Course
        course = new Course();
        course.setTitle("Software Engineering 101");
        course.setDescription("Introduction to SE");
        course.setInstructor(instructor);
        course = courseRepository.save(course);

        // Create Assignment
        CreateAssignmentRequest assignmentRequest = new CreateAssignmentRequest();
        assignmentRequest.setTitle("Lab 1: Design Patterns");
        assignmentRequest.setDescription("Implement Singleton Pattern");
        assignmentRequest.setDueDate(LocalDateTime.now().plusDays(7));
        assignmentRequest.setCourseId(course.getId());
        assignment = elearningService.createAssignment(assignmentRequest);
    }

    @Test
    void testEnrollmentAndPeerGradingFlow() {
        // Enroll students
        elearningService.enrollStudent(course.getId(), studentA.getEmail());
        elearningService.enrollStudent(course.getId(), studentB.getEmail());
        elearningService.enrollStudent(course.getId(), studentC.getEmail());

        // Student A submits assignment
        SubmitAssignmentRequest submitRequest = new SubmitAssignmentRequest();
        submitRequest.setAssignmentId(assignment.getId());
        submitRequest.setContent("https://github.com/studenta/lab1");
        Submission submission = elearningService.submitAssignment(submitRequest, studentA.getEmail());

        assertNotNull(submission);
        assertEquals("PENDING", submission.getStatus());
        assertNull(submission.getGrade());

        // Verify that 2 PeerReviews are created and the reviewers are B and C (A is excluded)
        List<PeerReview> reviews = peerReviewRepository.findBySubmissionId(submission.getId());
        assertEquals(2, reviews.size());

        PeerReview reviewForB = null;
        PeerReview reviewForC = null;

        for (PeerReview r : reviews) {
            if (r.getReviewer().getEmail().equalsIgnoreCase(studentB.getEmail())) {
                reviewForB = r;
            } else if (r.getReviewer().getEmail().equalsIgnoreCase(studentC.getEmail())) {
                reviewForC = r;
            }
        }

        assertNotNull(reviewForB, "Student B should be assigned as reviewer");
        assertNotNull(reviewForC, "Student C should be assigned as reviewer");

        // Reviewer B grades submission (score: 8.0)
        GradeReviewRequest gradeRequestB = new GradeReviewRequest();
        gradeRequestB.setGrade(8.0);
        PeerReview gradedB = elearningService.gradeReview(reviewForB.getId(), gradeRequestB, studentB.getEmail());
        assertEquals(8.0, gradedB.getGrade());

        // Submission should still be PENDING and grade is null because C hasn't graded yet
        submission = submissionRepository.findById(submission.getId()).orElseThrow();
        assertEquals("PENDING", submission.getStatus());
        assertNull(submission.getGrade());

        // Reviewer C grades submission (score: 9.0)
        GradeReviewRequest gradeRequestC = new GradeReviewRequest();
        gradeRequestC.setGrade(9.0);
        PeerReview gradedC = elearningService.gradeReview(reviewForC.getId(), gradeRequestC, studentC.getEmail());
        assertEquals(9.0, gradedC.getGrade());

        // Submission should now be COMPLETED and grade is 8.5 (average of 8.0 and 9.0)
        submission = submissionRepository.findById(submission.getId()).orElseThrow();
        assertEquals("COMPLETED", submission.getStatus());
        assertEquals(8.5, submission.getGrade());
    }

    @Test
    void testNotEnoughReviewersException() {
        // Enroll only student A and student B (fewer than 3 students in total, meaning only 1 potential classmate)
        elearningService.enrollStudent(course.getId(), studentA.getEmail());
        elearningService.enrollStudent(course.getId(), studentB.getEmail());

        SubmitAssignmentRequest submitRequest = new SubmitAssignmentRequest();
        submitRequest.setAssignmentId(assignment.getId());
        submitRequest.setContent("Student A's work");

        // Should throw BusinessException with code 400 (Không đủ người chấm)
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            elearningService.submitAssignment(submitRequest, studentA.getEmail());
        });

        assertEquals(400, exception.getCode());
        assertEquals("Không đủ người chấm trong khóa học", exception.getMessage());
    }

    @Test
    void testInvalidReviewerGradingException() {
        // Enroll all students
        elearningService.enrollStudent(course.getId(), studentA.getEmail());
        elearningService.enrollStudent(course.getId(), studentB.getEmail());
        elearningService.enrollStudent(course.getId(), studentC.getEmail());

        // Student A submits
        SubmitAssignmentRequest submitRequest = new SubmitAssignmentRequest();
        submitRequest.setAssignmentId(assignment.getId());
        submitRequest.setContent("Student A's work");
        Submission submission = elearningService.submitAssignment(submitRequest, studentA.getEmail());

        List<PeerReview> reviews = peerReviewRepository.findBySubmissionId(submission.getId());
        PeerReview review = reviews.get(0); // This belongs to either B or C

        // Let's identify the reviewer and attempt to grade using student A's email (owner of submission, not authorized)
        GradeReviewRequest gradeRequest = new GradeReviewRequest();
        gradeRequest.setGrade(7.0);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            elearningService.gradeReview(review.getId(), gradeRequest, studentA.getEmail());
        });

        assertEquals(403, exception.getCode());
        assertEquals("Người chấm không hợp lệ", exception.getMessage());
    }
}

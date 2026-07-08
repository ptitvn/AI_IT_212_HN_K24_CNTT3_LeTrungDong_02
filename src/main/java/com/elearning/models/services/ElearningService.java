package com.elearning.models.services;

import com.elearning.exceptions.BusinessException;
import com.elearning.models.entities.*;
import com.elearning.models.repositories.*;
import com.elearning.models.dto.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ElearningService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final PeerReviewRepository peerReviewRepository;

    @Transactional
    public void enrollStudent(Long courseId, String email) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(404, "Course not found"));
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(404, "Student not found with email: " + email));
        
        if (!course.getStudents().contains(student)) {
            course.getStudents().add(student);
            courseRepository.save(course);
        }
    }

    @Transactional
    public Assignment createAssignment(CreateAssignmentRequest request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new BusinessException(404, "Course not found"));
        
        Assignment assignment = new Assignment();
        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setDueDate(request.getDueDate() != null ? request.getDueDate() : LocalDateTime.now().plusDays(7));
        assignment.setCourse(course);
        
        return assignmentRepository.save(assignment);
    }

    @Transactional
    public Submission submitAssignment(SubmitAssignmentRequest request, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new BusinessException(404, "Student not found"));
        
        Assignment assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new BusinessException(404, "Assignment not found"));
        
        Course course = assignment.getCourse();
        if (!course.getStudents().contains(student)) {
            throw new BusinessException(403, "Học viên chưa tham gia khóa học này");
        }

        // Check if student already submitted
        if (submissionRepository.findByAssignmentIdAndStudentId(assignment.getId(), student.getId()).isPresent()) {
            throw new BusinessException(400, "Bạn đã nộp bài tập này rồi");
        }

        // Pick 2 random reviewers from classmates in the same course
        List<User> classmates = new ArrayList<>(course.getStudents());
        classmates.remove(student); // Reviewer cannot be the submitter

        if (classmates.size() < 2) {
            throw new BusinessException(400, "Không đủ người chấm trong khóa học");
        }

        // Shuffle to randomize
        Collections.shuffle(classmates);
        User reviewer1 = classmates.get(0);
        User reviewer2 = classmates.get(1);

        // Create Submission
        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setContent(request.getContent());
        submission.setGrade(null);
        submission.setStatus("PENDING");
        submission.setSubmittedAt(LocalDateTime.now());
        
        Submission savedSubmission = submissionRepository.save(submission);

        // Create 2 PeerReviews
        PeerReview review1 = new PeerReview();
        review1.setSubmission(savedSubmission);
        review1.setReviewer(reviewer1);
        peerReviewRepository.save(review1);

        PeerReview review2 = new PeerReview();
        review2.setSubmission(savedSubmission);
        review2.setReviewer(reviewer2);
        peerReviewRepository.save(review2);

        return savedSubmission;
    }

    @Transactional
    public PeerReview gradeReview(Long reviewId, GradeReviewRequest request, String reviewerEmail) {
        PeerReview peerReview = peerReviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(404, "Không tìm thấy lượt chấm chéo"));

        Submission submission = peerReview.getSubmission();

        // Security check: Reviewer must match current user, and cannot be the owner of the submission
        if (!peerReview.getReviewer().getEmail().equalsIgnoreCase(reviewerEmail)) {
            throw new BusinessException(403, "Người chấm không hợp lệ");
        }
        if (submission.getStudent().getEmail().equalsIgnoreCase(reviewerEmail)) {
            throw new BusinessException(403, "Người chấm không hợp lệ");
        }

        peerReview.setGrade(request.getGrade());
        peerReview.setReviewedAt(LocalDateTime.now());
        peerReviewRepository.save(peerReview);

        // Check if both reviewers completed their grades
        List<PeerReview> reviews = peerReviewRepository.findBySubmissionId(submission.getId());
        boolean allGraded = true;
        double sum = 0.0;
        for (PeerReview r : reviews) {
            if (r.getId().equals(reviewId)) {
                // Ensure current grade is used even if not persisted to database yet
                r.setGrade(request.getGrade());
            }
            if (r.getGrade() == null) {
                allGraded = false;
            } else {
                sum += r.getGrade();
            }
        }

        if (allGraded && !reviews.isEmpty()) {
            submission.setGrade(sum / reviews.size());
            submission.setStatus("COMPLETED");
            submissionRepository.save(submission);
        }

        return peerReview;
    }
}

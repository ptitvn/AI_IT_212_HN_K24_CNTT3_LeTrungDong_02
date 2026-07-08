package com.elearning.controllers;

import com.elearning.advice.ApiResponse;
import com.elearning.models.dto.*;
import com.elearning.models.entities.*;
import com.elearning.models.services.ElearningService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ElearningController {

    private final ElearningService elearningService;

    @PostMapping("/courses/{courseId}/enroll")
    public ResponseEntity<ApiResponse<Void>> enrollStudent(
            @PathVariable Long courseId,
            @RequestParam(required = false) String email,
            Principal principal) {
        
        String targetEmail = (email != null && !email.trim().isEmpty()) ? email : principal.getName();
        elearningService.enrollStudent(courseId, targetEmail);
        return ResponseEntity.ok(ApiResponse.success(null, "Student enrolled successfully"));
    }

    @PostMapping("/assignments")
    public ResponseEntity<ApiResponse<Assignment>> createAssignment(@RequestBody CreateAssignmentRequest request) {
        Assignment assignment = elearningService.createAssignment(request);
        return ResponseEntity.ok(ApiResponse.success(assignment, "Assignment created successfully"));
    }

    @PostMapping("/submissions")
    public ResponseEntity<ApiResponse<Submission>> submitAssignment(
            @RequestBody SubmitAssignmentRequest request,
            Principal principal) {
        
        Submission submission = elearningService.submitAssignment(request, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(submission, "Submission uploaded and peer reviewers assigned successfully"));
    }

    @PostMapping("/peer-reviews/{reviewId}/grade")
    public ResponseEntity<ApiResponse<PeerReview>> gradeReview(
            @PathVariable Long reviewId,
            @RequestBody GradeReviewRequest request,
            Principal principal) {
        
        PeerReview peerReview = elearningService.gradeReview(reviewId, request, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(peerReview, "Peer review graded successfully"));
    }
}

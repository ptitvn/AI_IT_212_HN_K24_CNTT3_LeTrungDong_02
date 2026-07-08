package com.elearning.models.repositories;

import com.elearning.models.entities.PeerReview;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PeerReviewRepository extends JpaRepository<PeerReview, Long> {
    List<PeerReview> findBySubmissionId(Long submissionId);
    List<PeerReview> findByReviewerEmail(String email);
}

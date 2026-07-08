package com.elearning.models.dto;

import lombok.Data;

@Data
public class SubmitAssignmentRequest {
    private Long assignmentId;
    private String content;
}

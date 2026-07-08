package com.elearning.models.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateAssignmentRequest {
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private Long courseId;
}

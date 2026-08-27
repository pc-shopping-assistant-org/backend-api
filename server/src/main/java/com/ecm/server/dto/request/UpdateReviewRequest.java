package com.ecm.server.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReviewRequest {

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer rating;

    @Size(max = 2000, message = "Comment cannot exceed 2000 characters")
    private String comment;
}

package com.ss.shopai.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductComparisonRequest {

    @NotEmpty(message = "At least 2 product ids are required")
    @Size(min = 2, max = 5, message = "You can compare between 2 and 5 products at a time")
    private List<Long> productIds;
}
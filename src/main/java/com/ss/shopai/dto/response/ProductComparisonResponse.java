package com.ss.shopai.dto.response;

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
public class ProductComparisonResponse {

    private List<ProductResponse> products;

    /** Attribute label -> list of values (same order as products), for easy table rendering */
    private java.util.Map<String, List<String>> attributeMatrix;
}
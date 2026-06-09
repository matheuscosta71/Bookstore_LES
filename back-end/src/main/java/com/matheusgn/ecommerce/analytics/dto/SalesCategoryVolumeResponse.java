package com.matheusgn.ecommerce.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesCategoryVolumeResponse {

    private List<String> labels; // e.g., ["2025-01", "2025-02", ...]
    private List<CategoryVolumeSeries> series;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryVolumeSeries {
        private String category;
        private List<Integer> volumes; // volume per month matching labels
    }
}

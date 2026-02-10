package com.example.ForDay.domain.app.dto.request;

import com.example.ForDay.domain.app.type.ImageUsageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeneratePresignedReqDto {

    private List<ImagePresignInfoDto> images;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ImagePresignInfoDto {
        private String originalFilename;
        private String contentType;   // image/jpeg
        private ImageUsageType usage;
        private int order;
    }
}
package com.example.ForDay.domain.user.dto.response;

import com.example.ForDay.domain.hobby.type.HobbyStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetHobbyInProgressResDto {
    private int inProgressHobbyCount;
    private int hobbyCardCount;
    private List<HobbyDto> hobbyList;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HobbyDto {
        private Long hobbyId;
        private String hobbyName;
        private String thumbnailImageUrl;
        private HobbyStatus status;
    }
}

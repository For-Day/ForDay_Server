package com.example.ForDay.domain.user.dto.response;

import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.hobby.utils.HobbyUtil;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.type.HobbyInfoImageIcon;
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

    public static GetHobbyInProgressResDto of(User user, List<HobbyDto> hobbyList) {
        int inProgressCount = (int) hobbyList.stream()
                .filter(h -> h.getStatus() == HobbyStatus.IN_PROGRESS)
                .count();

        return new GetHobbyInProgressResDto(
                inProgressCount,
                user.getHobbyCardCount(),
                hobbyList
        );
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HobbyDto {
        private Long hobbyId;
        private String hobbyName;
        private String thumbnailImageUrl;
        private HobbyStatus status;
        private Long hobbyInfoId;
        private HobbyInfoImageIcon imageCode;

        public HobbyDto(Long hobbyId, String hobbyName, String thumbnailImageUrl, HobbyStatus status, Long hobbyInfoId) {
            this.hobbyId = hobbyId;
            this.hobbyName = hobbyName;
            this.thumbnailImageUrl = thumbnailImageUrl;
            this.status = status;
            this.hobbyInfoId = hobbyInfoId;
            this.imageCode = HobbyUtil.mapImageCode(hobbyInfoId);
        }
    }
}

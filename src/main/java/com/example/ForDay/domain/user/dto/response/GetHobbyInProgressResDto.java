package com.example.ForDay.domain.user.dto.response;

import com.example.ForDay.domain.hobby.type.HobbyStatus;
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
            this.imageCode = mapImageCode(hobbyInfoId);
        }

        private HobbyInfoImageIcon mapImageCode(Long hobbyInfoId) {
            if (hobbyInfoId == null) return HobbyInfoImageIcon.DEFAULT_ICON;

            return switch (hobbyInfoId.intValue()) {
                case 1 -> HobbyInfoImageIcon.DRAWING_ICON;
                case 2 -> HobbyInfoImageIcon.GYM_ICON;
                case 3 -> HobbyInfoImageIcon.READING_ICON;
                case 4 -> HobbyInfoImageIcon.MUSIC_ICON;
                case 5 -> HobbyInfoImageIcon.RUNNING_ICON;
                case 6 -> HobbyInfoImageIcon.COOKING_ICON;
                case 7 -> HobbyInfoImageIcon.CAFE_ICON;
                case 8 -> HobbyInfoImageIcon.MOVIE_ICON;
                case 9 -> HobbyInfoImageIcon.PHOTO_ICON;
                case 10 -> HobbyInfoImageIcon.WRITING_ICON;

                default -> HobbyInfoImageIcon.DEFAULT_ICON;
            };
        }
    }
}

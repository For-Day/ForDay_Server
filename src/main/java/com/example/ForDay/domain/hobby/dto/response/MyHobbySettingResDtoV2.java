package com.example.ForDay.domain.hobby.dto.response;

import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.hobby.utils.HobbyUtil;
import com.example.ForDay.domain.user.type.HobbyInfoImageIcon;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyHobbySettingResDtoV2 {
    // 진행 중은 사용자가 지정한 sequence 순서에 따라
    List<ProgressHobbyList> progressHobbyList;
    // 숨김은 최신순으로
    List<HiddenHobbyList> hiddenHobbyList;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProgressHobbyList {
        private Long hobbyId;
        private String hobbyName;
        private HobbyStatus status;
        private HobbyInfoImageIcon imageIcon;
        private LocalDateTime createdAt;

        public static ProgressHobbyList from(Hobby hobby) {
            return new ProgressHobbyList(
                    hobby.getId(),
                    hobby.getHobbyName(),
                    hobby.getStatus(),
                    HobbyUtil.mapImageCode(hobby.getHobbyInfoId()),
                    hobby.getCreatedAt()
            );
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HiddenHobbyList {
        private Long hobbyId;
        private String hobbyName;
        private HobbyStatus status;
        private HobbyInfoImageIcon imageIcon;
        private LocalDateTime createdAt;

        public static HiddenHobbyList from(Hobby hobby) {
            return new HiddenHobbyList(
                    hobby.getId(),
                    hobby.getHobbyName(),
                    hobby.getStatus(),
                    HobbyUtil.mapImageCode(hobby.getHobbyInfoId()),
                    hobby.getCreatedAt()
            );
        }
    }
}

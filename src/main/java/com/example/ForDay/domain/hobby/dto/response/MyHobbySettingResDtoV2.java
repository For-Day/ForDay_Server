package com.example.ForDay.domain.hobby.dto.response;

import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyHobbySettingResDtoV2 {
    List<ProgressHobbyList> progressHobbyList;
    List<HiddenHobbyList> hiddenHobbyList;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProgressHobbyList {
        private Long hobbyId;
        private String hobbyName;
        private HobbyStatus status;

        public static ProgressHobbyList from(Hobby hobby) {
            return new ProgressHobbyList(
                    hobby.getId(),
                    hobby.getHobbyName(),
                    hobby.getStatus()
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

        public static HiddenHobbyList from(Hobby hobby) {
            return new HiddenHobbyList(
                    hobby.getId(),
                    hobby.getHobbyName(),
                    hobby.getStatus()
            );
        }
    }
}

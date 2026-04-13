package com.example.ForDay.domain.hobby.dto.response;

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
        private int sequence;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HiddenHobbyList {
        private Long hobbyId;
        private String hobbyName;
        private HobbyStatus status;
        private int sequence;
    }
}

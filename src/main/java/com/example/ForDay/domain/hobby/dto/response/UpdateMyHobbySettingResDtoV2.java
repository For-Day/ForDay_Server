package com.example.ForDay.domain.hobby.dto.response;

import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.hobby.utils.HobbyUtil;
import com.example.ForDay.domain.user.type.HobbyInfoImageIcon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateMyHobbySettingResDtoV2 {

    private List<ProgressHobbyList> progressHobbyList;
    private List<HiddenHobbyList> hiddenHobbyList;


    public static UpdateMyHobbySettingResDtoV2 from(List<Hobby> allHobbies) {
        List<ProgressHobbyList> progressList = allHobbies.stream()
                .filter(h -> h.getStatus() == HobbyStatus.IN_PROGRESS)
                .sorted(Comparator.comparing(Hobby::getSequence, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(ProgressHobbyList::from)
                .collect(Collectors.toList());

        List<HiddenHobbyList> hiddenList = allHobbies.stream()
                .filter(h -> h.getStatus() == HobbyStatus.ARCHIVED)
                .sorted(Comparator.comparing(Hobby::getCreatedAt).reversed())
                .map(HiddenHobbyList::from)
                .collect(Collectors.toList());

        return UpdateMyHobbySettingResDtoV2.builder()
                .progressHobbyList(progressList)
                .hiddenHobbyList(hiddenList)
                .build();
    }

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
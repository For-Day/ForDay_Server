package com.example.ForDay.domain.hobby.dto.response;

import com.example.ForDay.domain.hobby.entity.Hobby;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HobbyCreateResDtoV2 {
    private String message;
    private int createdHobbyCount;
    private List<CreatedHobbyInfo> createdHobbyInfoList;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CreatedHobbyInfo {
        private Long hobbyId;
        private Long hobbyInfoId;
        private String hobbyName;
    }

    public static HobbyCreateResDtoV2 from(List<Hobby> savedHobbies) {
        List<CreatedHobbyInfo> infoList = savedHobbies.stream()
                .map(hobby -> new CreatedHobbyInfo(
                        hobby.getId(),
                        hobby.getHobbyInfoId(),
                        hobby.getHobbyName()
                ))
                .collect(Collectors.toList());

        return HobbyCreateResDtoV2.builder()
                .message("취미 생성이 완료되었습니다.")
                .createdHobbyCount(infoList.size())
                .createdHobbyInfoList(infoList)
                .build();
    }
}
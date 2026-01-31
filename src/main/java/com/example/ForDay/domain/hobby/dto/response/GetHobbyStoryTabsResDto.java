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
public class GetHobbyStoryTabsResDto {
    List<HobbyTabInfoDto> tabInfo;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HobbyTabInfoDto{
        private Long hobbyId;
        private String hobbyName;
        private HobbyStatus hobbyStatus;

        public static HobbyTabInfoDto from(Hobby hobby) {
            return new HobbyTabInfoDto(
                    hobby.getId(),
                    hobby.getHobbyName(),
                    hobby.getStatus()
            );
        }
    }


}

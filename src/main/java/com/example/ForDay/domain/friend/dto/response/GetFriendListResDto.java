package com.example.ForDay.domain.friend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Function;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetFriendListResDto {
    private String message;
    private List<UserInfoDto> userInfo;
    private String lastUserId;
    private boolean hasNext;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfoDto {
        private String userId;
        private String nickname;
        private String profileImageUrl;

        public static UserInfoDto from(UserInfoDto dto, String profileUrl) {
            return new UserInfoDto(
                    dto.getUserId(),
                    dto.getNickname(),
                    profileUrl
            );
        }

        // 리스트 변환 및 S3 URL 처리를 위함
        public static List<UserInfoDto> listOf(List<UserInfoDto> dtos, Function<String, String> urlMapper) {
            return dtos.stream()
                    .map(dto -> UserInfoDto.from(dto, urlMapper.apply(dto.getProfileImageUrl())))
                    .toList();
        }
    }

    public static GetFriendListResDto of(String message, List<UserInfoDto> dtos, int size) {
        boolean hasNext = dtos.size() > size;
        List<UserInfoDto> resultList = hasNext ? dtos.subList(0, size) : dtos;

        String lastId = resultList.isEmpty() ? null : resultList.get(resultList.size() - 1).getUserId();

        return new GetFriendListResDto(message, resultList, lastId, hasNext);
    }
}

package com.example.ForDay.domain.recent.service;

import com.example.ForDay.domain.recent.dto.response.DeleteAllRecentKeywordResDto;
import com.example.ForDay.domain.recent.dto.response.DeleteRecentKeywordResDto;
import com.example.ForDay.domain.recent.dto.response.GetRecentKeywordResDto;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecentService {
    private final RecentRedisService recentRedisService;
    private final UserUtil userUtil;

    @Transactional(readOnly = true)
    public GetRecentKeywordResDto getRecentKeyword(CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        return recentRedisService.getRecentKeywords(currentUser.getId());
    }

    @Transactional
    public DeleteAllRecentKeywordResDto deleteAllRecentKeyword(CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        recentRedisService.deleteAllRecentKeywords(currentUser.getId());
        return new DeleteAllRecentKeywordResDto("전체 검색어가 삭제되었습니다.");
    }

    @Transactional
    public DeleteRecentKeywordResDto deleteRecentKeyword(Long recentId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        Long deletedId = recentRedisService.deleteRecentKeyword(currentUser.getId(), recentId);
        return new DeleteRecentKeywordResDto("개별 검색어가 삭제되었습니다.", deletedId);
    }
}

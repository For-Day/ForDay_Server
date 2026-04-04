package com.example.ForDay.domain.activity.service;

import com.example.ForDay.domain.activity.dto.response.GetAiRecommendItemsResDto;
import com.example.ForDay.domain.activity.entity.ActivityRecommendItem;
import com.example.ForDay.domain.activity.repository.ActivityRecommendItemRepository;
import com.example.ForDay.domain.activity.type.AIItemType;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.service.UserSummaryAIService;
import com.example.ForDay.domain.hobby.utils.HobbyUtil;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.constants.AiMessageConstants;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityRecommendItemService {
    private final UserUtil userUtil;
    private final HobbyUtil hobbyUtil;
    private final ActivityRecommendItemRepository recommendItemRepository;
    private final UserSummaryAIService userSummaryAIService;

    @Transactional(readOnly = true)
    public GetAiRecommendItemsResDto getAiRecommendItems(Long hobbyId, CustomUserDetails user, AIItemType type) {
        User currentUser = userUtil.getCurrentUser(user);
        log.info("[AI Recommend] 아이템 조회 시작 - HobbyId: {}, Type: {}", hobbyId, type);

        Hobby hobby = hobbyUtil.getHobbyByUserId(hobbyId, currentUser);
        List<ActivityRecommendItem> items = recommendItemRepository.findAllByHobbyIdAndDate(hobby.getId(), LocalDate.now().atStartOfDay(), LocalDate.now().atTime(LocalTime.MAX), type);

        if (items.isEmpty()) {
            return new GetAiRecommendItemsResDto();
        }
        String userSummaryText = AiMessageConstants.formatPreviousRecommendation(userSummaryAIService.determine(currentUser, hobby));

        return GetAiRecommendItemsResDto.of(hobby, items, userSummaryText);
    }
}

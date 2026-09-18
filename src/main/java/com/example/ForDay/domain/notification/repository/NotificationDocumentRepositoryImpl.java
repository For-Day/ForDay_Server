package com.example.ForDay.domain.notification.repository;

import com.example.ForDay.domain.notification.document.NotificationDocument;
import com.example.ForDay.domain.notification.dto.response.GetNotificationInfoResDto;
import com.example.ForDay.domain.notification.dto.response.GetNotificationListResDto;
import com.example.ForDay.domain.notification.type.NotificationFilterType;
import com.example.ForDay.domain.notification.type.NotificationType;
import com.example.ForDay.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class NotificationDocumentRepositoryImpl implements NotificationDocumentRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public GetNotificationListResDto getNotificationList(NotificationFilterType filterType, Long lastNotificationId, Integer pageSize, User currentUser) {
        Query query = new Query();
        query.addCriteria(Criteria.where("receiverId").is(currentUser.getId()));

        if (lastNotificationId != null) {
            query.addCriteria(Criteria.where("id").lt(lastNotificationId));
        }
        applyFilter(query, filterType);

        // limit(pageSize + 1)로 한 건 더 가져와 hasNext를 판정하는 방식은 기존 QueryDSL
        // 구현(NotificationRepositoryImpl)과 동일하다 - API 응답 형태를 그대로 유지한다.
        query.with(Sort.by(Sort.Direction.DESC, "id")).limit(pageSize + 1);

        List<NotificationDocument> results = mongoTemplate.find(query, NotificationDocument.class);

        boolean hasNext = results.size() > pageSize;
        List<NotificationDocument> content = hasNext ? results.subList(0, pageSize) : results;

        List<GetNotificationInfoResDto> infoList = content.stream()
                .map(this::convertToInfoDto)
                .collect(Collectors.toList());

        Long nextCursorId = content.isEmpty() ? null : content.get(content.size() - 1).getId();

        return new GetNotificationListResDto(
                GetNotificationListResDto.PushInfo.pushEnabled(),
                infoList,
                hasNext,
                nextCursorId != null ? String.valueOf(nextCursorId) : null
        );
    }

    @Override
    public void updateImageUrlByRecordId(Long recordId, String newImageUrl) {
        Query query = Query.query(Criteria.where("payload.recordId").is(recordId));
        Update update = new Update().set("imageUrl", newImageUrl);
        mongoTemplate.updateMulti(query, update, NotificationDocument.class);
    }

    private GetNotificationInfoResDto convertToInfoDto(NotificationDocument n) {
        GetNotificationInfoResDto dto = GetNotificationInfoResDto.from(n);

        Map<String, Object> payload = n.getPayload();
        if (payload != null) {
            if (n.getType() == NotificationType.RECORD_REACTION) {
                dto.setReactionAlram(GetNotificationInfoResDto.ReactionAlramDto.fromPayload(payload));
            } else if (n.getType() == NotificationType.RECORD_COMMENT) {
                dto.setCommentAlram(GetNotificationInfoResDto.CommentAlramDto.fromPayload(payload));
            }
        }

        return dto;
    }

    private void applyFilter(Query query, NotificationFilterType filterType) {
        if (filterType == null) {
            return;
        }
        switch (filterType) {
            case RECORD -> query.addCriteria(Criteria.where("type").in(NotificationType.RECORD_COMMENT, NotificationType.RECORD_REACTION));
            case FRIEND -> query.addCriteria(Criteria.where("type").is(NotificationType.FRIEND));
            case GROUP -> query.addCriteria(Criteria.where("type").is(NotificationType.GROUP));
            case ALL -> { /* 필터 없음 - 전체 조회 */ }
        }
    }
}

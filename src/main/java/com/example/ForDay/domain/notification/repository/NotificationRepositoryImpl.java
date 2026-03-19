package com.example.ForDay.domain.notification.repository;

import com.example.ForDay.domain.notification.dto.response.GetNotificationInfoResDto;
import com.example.ForDay.domain.notification.dto.response.GetNotificationListResDto;
import com.example.ForDay.domain.notification.entity.CommentNotification;
import com.example.ForDay.domain.notification.entity.Notification;
import com.example.ForDay.domain.notification.entity.QNotification;
import com.example.ForDay.domain.notification.entity.ReactionNotification;
import com.example.ForDay.domain.notification.type.NotificationFilterType;
import com.example.ForDay.domain.notification.type.NotificationType;
import com.example.ForDay.domain.user.entity.User;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public GetNotificationListResDto getNotificationList(NotificationFilterType filterType, Long lastNotificationId, Integer pageSize, User currentUser) {
        QNotification notification = QNotification.notification;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(notification.receiver.eq(currentUser));

        if (lastNotificationId != null) {
            builder.and(notification.id.lt(lastNotificationId));
        }

        if (filterType != null) {
            applyFilter(builder, filterType, notification);
        }

        List<Notification> results = queryFactory
                .selectFrom(notification)
                .where(builder)
                .orderBy(notification.id.desc())
                .limit(pageSize + 1)
                .fetch();

        boolean hasNext = results.size() > pageSize;
        List<Notification> content = hasNext ? results.subList(0, pageSize) : results;

        List<GetNotificationInfoResDto> infoList = content.stream()
                .map(this::convertToInfoDto)
                .collect(Collectors.toList());

        Long nextCursorId = content.isEmpty() ? null : content.get(content.size() - 1).getId();

        return new GetNotificationListResDto(
                infoList,
                hasNext,
                nextCursorId != null ? String.valueOf(nextCursorId) : null
        );
    }

    private GetNotificationInfoResDto convertToInfoDto(Notification n) {
        GetNotificationInfoResDto dto = new GetNotificationInfoResDto();
        dto.setNotificationId(n.getId());
        dto.setMessage(n.getMessage());
        dto.setType(n.getType());
        dto.setImageUrl(n.getSender() != null ? n.getSender().getProfileImageUrl() : null);
        dto.setRead(n.isRead());

        if (n instanceof ReactionNotification reaction) {
            dto.setReactionAlram(new GetNotificationInfoResDto.ReactionAlramDto(
                    reaction.getReactionType(),
                    reaction.getRecordId()
            ));
        } else if (n instanceof CommentNotification comment) {
            dto.setCommentAlram(new GetNotificationInfoResDto.CommentAlramDto(
                    comment.getRecordId(),
                    comment.getCommentId(),
                    comment.getCommentContent()
            ));
        }

        return dto;
    }

    private void applyFilter(BooleanBuilder builder, NotificationFilterType filterType, QNotification notification) {
        switch (filterType) {
            case RECORD -> builder.and(notification.type.in(NotificationType.RECORD_COMMENT, NotificationType.RECORD_REACTION));
            case FRIEND -> builder.and(notification.type.eq(NotificationType.FRIEND));
            case GROUP -> builder.and(notification.type.eq(NotificationType.GROUP));
        }
    }
}
package com.example.ForDay.domain.notification.repository;

import com.example.ForDay.domain.notification.document.NotificationDocument;
import com.example.ForDay.domain.notification.dto.response.GetNotificationListResDto;
import com.example.ForDay.domain.notification.type.NotificationFilterType;
import com.example.ForDay.domain.notification.type.NotificationType;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.domain.user.type.SocialType;
import com.example.ForDay.global.mongo.service.MongoSequenceGeneratorService;
import com.example.ForDay.support.IntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이슈 #408 - RDB 버전(NotificationRepositoryImpl)에는 없던, 커서 페이징/필터에 대한
 * 테스트를 새로 추가한다(기존에 커버되지 않던 영역).
 */
@Transactional
class NotificationDocumentRepositoryImplTest extends IntegrationTestSupport {

    @Autowired
    private NotificationDocumentRepository notificationDocumentRepository;

    @Autowired
    private MongoSequenceGeneratorService sequenceGeneratorService;

    @Autowired
    private UserRepository userRepository;

    private User receiver;
    private User otherUser;

    @BeforeEach
    void setUp() {
        receiver = userRepository.save(User.builder()
                .socialId("notif_cursor_receiver")
                .nickname("받는사람")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .build());
        otherUser = userRepository.save(User.builder()
                .socialId("notif_cursor_other")
                .nickname("다른사람")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .build());
    }

    @AfterEach
    void tearDown() {
        notificationDocumentRepository.deleteByReceiverId(receiver.getId());
        notificationDocumentRepository.deleteByReceiverId(otherUser.getId());
    }

    private Long saveNotification(User target, NotificationType type) {
        long id = sequenceGeneratorService.generateSequence(NotificationDocument.SEQUENCE_NAME);
        Map<String, Object> payload = type == NotificationType.RECORD_REACTION
                ? Map.of("reactionType", "GREAT", "recordId", 1L)
                : Map.of();
        NotificationDocument document = NotificationDocument.create(
                id, target.getId(), null, null, type, "메세지-" + id, null, payload);
        notificationDocumentRepository.save(document);
        return id;
    }

    @Nested
    @DisplayName("커서 페이징")
    class CursorPagination {

        @Test
        @DisplayName("pageSize보다 알림이 많으면 hasNext가 true이고, 커서로 다음 페이지를 이어 가져올 수 있다")
        void 다음_페이지를_커서로_이어_가져온다() {
            Long id1 = saveNotification(receiver, NotificationType.RECORD_REACTION);
            Long id2 = saveNotification(receiver, NotificationType.RECORD_REACTION);
            Long id3 = saveNotification(receiver, NotificationType.RECORD_REACTION);

            // 첫 페이지: 최신순(id desc)으로 2건 - id3, id2
            GetNotificationListResDto firstPage =
                    notificationDocumentRepository.getNotificationList(null, null, 2, receiver);

            assertThat(firstPage.isHasNext()).isTrue();
            assertThat(firstPage.getNotificationList()).hasSize(2);
            assertThat(firstPage.getNotificationList().get(0).getNotificationId()).isEqualTo(id3);
            assertThat(firstPage.getNotificationList().get(1).getNotificationId()).isEqualTo(id2);
            assertThat(firstPage.getLastNotificationId()).isEqualTo(String.valueOf(id2));

            // 두 번째 페이지: 커서(id2) 이후 - id1 하나만 남고 hasNext는 false
            GetNotificationListResDto secondPage =
                    notificationDocumentRepository.getNotificationList(null, id2, 2, receiver);

            assertThat(secondPage.isHasNext()).isFalse();
            assertThat(secondPage.getNotificationList()).hasSize(1);
            assertThat(secondPage.getNotificationList().get(0).getNotificationId()).isEqualTo(id1);
        }

        @Test
        @DisplayName("다른 유저의 알림은 섞이지 않는다")
        void 다른_유저_알림은_섞이지_않는다() {
            saveNotification(otherUser, NotificationType.RECORD_REACTION);
            Long myId = saveNotification(receiver, NotificationType.RECORD_REACTION);

            GetNotificationListResDto result =
                    notificationDocumentRepository.getNotificationList(null, null, 10, receiver);

            assertThat(result.getNotificationList()).hasSize(1);
            assertThat(result.getNotificationList().get(0).getNotificationId()).isEqualTo(myId);
        }
    }

    @Nested
    @DisplayName("타입 필터")
    class TypeFilter {

        @Test
        @DisplayName("RECORD 필터는 RECORD_REACTION/RECORD_COMMENT만 포함한다")
        void RECORD_필터는_반응과_댓글만_포함한다() {
            Long reactionId = saveNotification(receiver, NotificationType.RECORD_REACTION);
            saveNotification(receiver, NotificationType.FRIEND);
            saveNotification(receiver, NotificationType.GROUP);

            GetNotificationListResDto result = notificationDocumentRepository.getNotificationList(
                    NotificationFilterType.RECORD, null, 10, receiver);

            assertThat(result.getNotificationList()).hasSize(1);
            assertThat(result.getNotificationList().get(0).getNotificationId()).isEqualTo(reactionId);
        }

        @Test
        @DisplayName("filterType이 null이면 전체를 반환한다")
        void 필터가_없으면_전체를_반환한다() {
            saveNotification(receiver, NotificationType.RECORD_REACTION);
            saveNotification(receiver, NotificationType.FRIEND);

            GetNotificationListResDto result =
                    notificationDocumentRepository.getNotificationList(null, null, 10, receiver);

            assertThat(result.getNotificationList()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("REACTION payload 조립")
    class ReactionPayload {

        @Test
        @DisplayName("REACTION 타입은 reactionAlram에 payload 값이 채워진다")
        void reactionAlram이_채워진다() {
            saveNotification(receiver, NotificationType.RECORD_REACTION);

            GetNotificationListResDto result =
                    notificationDocumentRepository.getNotificationList(null, null, 10, receiver);

            var info = result.getNotificationList().get(0);
            assertThat(info.getReactionAlram()).isNotNull();
            assertThat(info.getReactionAlram().getRecordId()).isEqualTo(1L);
            assertThat(info.getCommentAlram()).isNull();
        }
    }
}

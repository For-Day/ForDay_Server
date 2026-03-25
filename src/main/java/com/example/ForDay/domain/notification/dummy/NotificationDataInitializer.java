package com.example.ForDay.domain.notification.dummy;

import com.example.ForDay.domain.notification.entity.CommentNotification;
import com.example.ForDay.domain.notification.entity.ReactionNotification;
import com.example.ForDay.domain.notification.repository.NotificationRepository;
import com.example.ForDay.domain.notification.type.NotificationType;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

//@Component
@RequiredArgsConstructor
@Profile("local")
public class NotificationDataInitializer implements CommandLineRunner {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        User testUser = userRepository.findBySocialId("guest_8d028cfe-25da-4153-bdb5-5e2f9bb95d40");
        if (testUser == null) return;

        for (int i = 1; i <= 15; i++) {
            ReactionNotification reaction = ReactionNotification.create(
                    testUser,
                    null,
                    NotificationType.RECORD_REACTION,
                    "테스트 유저님이 " + i + "번째 기록에 좋아요를 남겼어요.",
                    RecordReactionType.GREAT,
                    (long) i
            );
            notificationRepository.save(reaction);

            CommentNotification comment = CommentNotification.create(
                    testUser,
                    null,
                    NotificationType.RECORD_COMMENT,
                    i + "번째 기록에 새 댓글이 달렸습니다.",
                    (long) i,
                    (long) (100 + i),
                    "이것은 " + i + "번째 테스트 댓글 내용입니다."
            );
            notificationRepository.save(comment);
        }

        System.out.println(">>> 알림 더미 데이터 30개 생성 완료 (정적 메서드 방식)");
    }
}
package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.domain.user.type.SocialType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ActivityRecordScrapRepositoryImplTest {
    @Autowired
    private ActivityRecordRepository activityRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void 다른사용자의_스크랩목록을_조회하는_경우() {
        User userA = User.builder()
                .nickname("유저A")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_a")
                .deleted(false)
                .build();
        userRepository.save(userA);

        User userB = User.builder()
                .nickname("유저B")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_b")
                .deleted(false)
                .build();
        userRepository.save(userB);

        User userC = User.builder()
                .nickname("유저C")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_c")
                .deleted(false)
                .build();
        userRepository.save(userC);

        User userD = User.builder()
                .nickname("유저D")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_d")
                .deleted(false)
                .build();
        userRepository.save(userD);

        User userE = User.builder()
                .nickname("유저E")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_e")
                .deleted(false)
                .build();
        userRepository.save(userE);

        User userF = User.builder()
                .nickname("유저F")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_f")
                .deleted(false)
                .build();
        userRepository.save(userF);

        User userG = User.builder()
                .nickname("유저G")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_g")
                .deleted(false)
                .build();
        userRepository.save(userG);

        Activity.builder()
                .user(userA)
                .ho
    }

}
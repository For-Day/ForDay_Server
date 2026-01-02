package com.example.ForDay.domain.user.service;

import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.domain.user.type.SocialType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest(properties = "spring.profiles.active=test")
@ActiveProfiles("test")
class GuestCleanUpServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GuestCleanUpService guestCleanupService;

    @Test
    void deleteOldGuests_removesOnlyOldGuestUsers() {

        // given
        User oldGuest = User.builder()
                .email("old@guest.com")
                .role(Role.GUEST)
                .socialId("guest_old")
                .socialType(SocialType.GUEST)
                .lastActivityAt(LocalDateTime.now().minusMonths(7))
                .build();

        User activeGuest = User.builder()
                .email("active@guest.com")
                .role(Role.GUEST)
                .socialId("guest_active")
                .socialType(SocialType.GUEST)
                .lastActivityAt(LocalDateTime.now().minusMonths(1))
                .build();

        userRepository.save(oldGuest);
        userRepository.save(activeGuest);

        // when
        guestCleanupService.deleteOldGuests();

        // then
        List<User> users = userRepository.findAll();

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getSocialId()).isEqualTo("guest_active");
    }

}
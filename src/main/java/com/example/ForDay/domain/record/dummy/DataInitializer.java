package com.example.ForDay.domain.record.dummy;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

//@Component
@Profile("local")
@RequiredArgsConstructor
public class DataInitializer {

    private final ActivityRecordRepository activityRecordRepository;
    private final UserRepository userRepository;
    private final HobbyRepository hobbyRepository;
    private final ActivityRepository activityRepository;

    private void insertDummyRecords() {
        User user = userRepository.findById("7746f373-4dea-41af-8512-b3a3ad3f2608")
                .orElseThrow(() -> new IllegalStateException("User not found"));

        // hobby 1 → activity 1,2,3
        createRecords(user, 1L, List.of(1L, 2L, 3L));

        // hobby 2 → activity 4~9
        createRecords(user, 2L, List.of(4L, 5L, 6L, 7L, 8L, 9L));
    }

    private void createRecords(
            User user,
            Long hobbyId,
            List<Long> activityIds
    ) {
        Hobby hobby = hobbyRepository.findById(hobbyId)
                .orElseThrow(() -> new IllegalStateException("Hobby not found: " + hobbyId));

        for (Long activityId : activityIds) {
            Activity activity = activityRepository.findById(activityId)
                    .orElseThrow(() -> new IllegalStateException("Activity not found: " + activityId));

            for (int i = 1; i <= 3; i++) {
                ActivityRecord record = ActivityRecord.builder()
                        .user(user)
                        .hobby(hobby)
                        .activity(activity)
                        .sticker("STICKER_" + i)
                        .memo("activity " + activityId + " record " + i)
                        .visibility(RecordVisibility.PUBLIC)
                        .imageUrl("https://dummy.image/activity_" + activityId + "_" + i + ".jpg")
                        .build();

                activityRecordRepository.save(record);
            }
        }
    }
}
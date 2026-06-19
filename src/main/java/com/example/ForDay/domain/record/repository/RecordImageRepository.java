package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.record.entity.RecordImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecordImageRepository extends JpaRepository<RecordImage, Long> {
    List<RecordImage> findAllByActivityRecordIdOrderByImageOrderAsc(Long activityRecordId);
}

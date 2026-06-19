package com.example.ForDay.domain.record.entity;

import com.example.ForDay.domain.record.dto.request.ActivityRecordReqDtoV2;
import com.example.ForDay.global.common.mapped.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "record_images")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_record_id", nullable = false)
    private ActivityRecord activityRecord;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private Integer imageOrder;

    @Column(nullable = false)
    private Long imageWidth;

    @Column(nullable = false)
    private Long imageHeight;

    @Column(nullable = false)
    @Builder.Default
    private boolean thumbnail = false;

    public static RecordImage of(ActivityRecord activityRecord, ActivityRecordReqDtoV2.ActivityImageReqDto image, boolean thumbnail) {
        return RecordImage.builder()
                .activityRecord(activityRecord)
                .imageUrl(image.getImageUrl())
                .imageOrder(image.getImageOrder())
                .imageWidth(image.getImageWidth())
                .imageHeight(image.getImageHeight())
                .thumbnail(thumbnail)
                .build();
    }
}
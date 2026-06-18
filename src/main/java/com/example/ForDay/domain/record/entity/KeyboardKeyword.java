package com.example.ForDay.domain.record.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "keyboard_keywords",
        indexes = {
                @Index(name = "idx_keyboard_keyword_hobby_info_id", columnList = "hobby_info_id")
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeyboardKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "keyword_id")
    private Long id;

    @Column(name = "hobby_info_id", nullable = false)
    private Long hobbyInfoId;

    @Column(name = "keyword", nullable = false, length = 50)
    private String keyword;
}

package com.example.ForDay.domain.hobby.entity;

import com.example.ForDay.global.common.mapped.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Entity
@Table(name = "hobby_purposes")
@Getter
@AllArgsConstructor
@Builder
public class HobbyPurpose extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hobby_purpose")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_hobby_id", nullable = false)
    private Hobby hobby;

    @Column(name = "content", nullable = false, length = 50)
    private String content;

    protected void setHobby(Hobby hobby) {
        this.hobby = hobby;
    }

}

package com.example.ForDay.domain.hobby.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "hobby_cards")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HobbyCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hobby_card_id")
    private Long id;

    @Column(name = "hobby_name", nullable = false, length = 20)
    private String hobbyName;

    @Column(name = "hobby_description", nullable = false, length = 50)
    private String hobbyDescription;

    @Column(name = "image_code", nullable = false, length = 20)
    private String imageCode;
}

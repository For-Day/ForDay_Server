package com.example.ForDay.domain.app.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_version")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "app_version_id")
    private Long id;
    private String description;
    private String version;
    private LocalDateTime createdAt;
}

package com.example.ForDay.domain.record.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_record_count")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRecordCount {
    @Id
    @Column(name = "user_id")
    private String id;

    private int recordCount;
}

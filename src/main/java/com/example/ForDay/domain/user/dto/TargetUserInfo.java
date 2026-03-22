package com.example.ForDay.domain.user.dto;

import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.user.entity.User;

import java.util.List;

public record TargetUserInfo(User user, List<RecordVisibility> visibilities) {
}

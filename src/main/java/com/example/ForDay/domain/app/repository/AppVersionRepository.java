package com.example.ForDay.domain.app.repository;

import com.example.ForDay.domain.app.entity.AppVersion;
import com.example.ForDay.domain.app.type.Platform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AppVersionRepository extends JpaRepository<AppVersion, Long> {
    Optional<AppVersion> findFirstByPlatformOrderByCreatedAtDesc(Platform platform);
}

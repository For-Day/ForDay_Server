package com.example.ForDay.domain.app.repository;

import com.example.ForDay.domain.app.entity.AppVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AppVersionRepository extends JpaRepository<AppVersion, Long> {
    @Query("SELECT av FROM AppVersion av ORDER BY av.createdAt DESC LIMIT 1")
    Optional<AppVersion> findLatestVersion();
}

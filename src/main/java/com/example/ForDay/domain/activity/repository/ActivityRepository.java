package com.example.ForDay.domain.activity.repository;


import com.example.ForDay.domain.activity.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, Long>, ActivityRepositoryCustom{
}

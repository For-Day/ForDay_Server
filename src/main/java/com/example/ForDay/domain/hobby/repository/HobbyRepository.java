package com.example.ForDay.domain.hobby.repository;

import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HobbyRepository extends JpaRepository<Hobby, Long>, HobbyRepositoryCustom {
    long countByStatusAndUser(HobbyStatus hobbyStatus, User currentUser);
}

package com.example.ForDay.domain.hobby.repository;

import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HobbyRepository extends JpaRepository<Hobby, Long>, HobbyRepositoryCustom {
    long countByStatusAndUser(HobbyStatus hobbyStatus, User currentUser);

    Optional<Hobby> findTopByUserIdAndStatusOrderByCreatedAtDesc(String userId, HobbyStatus status);

    @Query("SELECT h FROM Hobby h WHERE h.id = :hobbyId AND h.user.id = :userId")
    Optional<Hobby> findByIdAndUserId(@Param("hobbyId") Long hobbyId, @Param("userId") String userId);

    @Query("SELECT COUNT(h) > 0 FROM Hobby h WHERE h.id = :hobbyId AND h.user.id = :userId")
    boolean existsByIdAndUserId(@Param("hobbyId") Long hobbyId, @Param("userId") String userId);

    @Query("SELECT COUNT(h) > 0 FROM Hobby h WHERE h.id = :hobbyId AND h.user.id = :userId AND h.status = :status")
    boolean existsByIdAndUserIdAndStatus(
            @Param("hobbyId") Long hobbyId,
            @Param("userId") String userId,
            @Param("status") HobbyStatus status
    );

    @Query("SELECT SUM(h.currentStickerNum) FROM Hobby h WHERE h.user.id = :userId")
    Optional<Integer> sumCurrentStickerNumByUserId(@Param("userId") String userId);

    List<Hobby> findAllByUserIdAndStatusOrderByIdDesc(String currentUserId, HobbyStatus hobbyStatus);

    Optional<Hobby> findByIdAndUserIdAndStatus(Long hobbyId, String id, HobbyStatus hobbyStatus);

    boolean existsByHobbyInfoIdAndUserId(Long hobbyInfoId, String id);

    boolean existsByHobbyNameAndUserId(String hobbyName, String id);
}

package com.example.ForDay.domain.friend.repository;

import com.example.ForDay.domain.friend.dto.response.GetFriendListResDto;
import com.example.ForDay.domain.friend.entity.FriendRelation;
import com.example.ForDay.domain.friend.type.FriendRelationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendRelationRepository extends JpaRepository<FriendRelation, Long>, FriendRelationRepositoryCustom {
    @Query("SELECT COUNT(f) > 0 FROM FriendRelation f " +
            "WHERE f.relationStatus = 'FOLLOW' " +
            "AND ((f.requester.id = :writerId AND f.targetUser.id = :currentUserId) " +
            "OR (f.requester.id = :currentUserId AND f.targetUser.id = :writerId))")
    boolean existsAcceptedFriendship(@Param("writerId") String writerId, @Param("currentUserId") String currentUserId);

    Optional<FriendRelation> findByRequesterIdAndTargetUserId(String id, String id1);

    boolean existsByRequesterIdAndTargetUserId(String currentUserId, String targetId);

    boolean existsByRequesterIdAndTargetUserIdAndRelationStatus(String id, String id1, FriendRelationStatus friendRelationStatus);
}

package com.example.ForDay.domain.friend.repository;

import com.example.ForDay.domain.friend.entity.FriendRelation;
import com.example.ForDay.domain.friend.type.FriendRelationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendRelationRepository extends JpaRepository<FriendRelation, Long>, FriendRelationRepositoryCustom {
    Optional<FriendRelation> findByRequesterIdAndTargetUserId(String id, String id1);

    boolean existsByRequesterIdAndTargetUserIdAndRelationStatus(String id, String id1, FriendRelationStatus friendRelationStatus);

    @Query("SELECT COUNT(f) > 0 FROM FriendRelation f " +
            "WHERE f.requester.id = :requesterId " +
            "AND f.targetUser.id = :targetId " +
            "AND f.relationStatus = :status")
    boolean existsByFriendship(
            @Param("requesterId") String requesterId,
            @Param("targetId") String targetId,
            @Param("status") FriendRelationStatus status
    );

    @Query("SELECT CASE WHEN f.requester.id = :userId THEN f.targetUser.id ELSE f.requester.id END " +
            "FROM FriendRelation f " +
            "WHERE (f.requester.id = :userId OR f.targetUser.id = :userId) " +
            "AND f.relationStatus = 'FOLLOW'")
    List<String> findAllFriendIdsByUserId(@Param("userId") String userId);

    @Query("SELECT f FROM FriendRelation f " +
            "WHERE (f.requester.id = :uid1 AND f.targetUser.id = :uid2) " +
            "OR (f.requester.id = :uid2 AND f.targetUser.id = :uid1)")
    List<FriendRelation> findAllRelationsBetween(@Param("uid1") String uid1, @Param("uid2") String uid2);
}

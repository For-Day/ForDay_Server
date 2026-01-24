package com.example.ForDay.domain.friend;

import com.example.ForDay.domain.friend.entity.FriendRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendRelationRepository extends JpaRepository<FriendRelation, Long> {
    @Query("SELECT COUNT(f) > 0 FROM FriendRelation f " +
            "WHERE f.relationStatus = 'ACCEPTED' " +
            "AND ((f.requesterId.id = :writerId AND f.targetId.id = :currentUserId) " +
            "OR (f.requesterId.id = :currentUserId AND f.targetId.id = :writerId))")
    boolean existsAcceptedFriendship(@Param("writerId") String writerId,
                                     @Param("currentUserId") String currentUserId);
}

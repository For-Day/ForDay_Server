package com.example.ForDay.domain.reaction.repository;

import com.example.ForDay.domain.reaction.entity.ActivityRecordReactionCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityRecordReactionCountRepository extends JpaRepository<ActivityRecordReactionCount, Long> {

    @Modifying
    @Query(value = "UPDATE record_reaction_count " +
            "SET total_count = total_count + 1, " +
            "awesome_count = awesome_count + CASE WHEN :type = 'AWESOME' THEN 1 ELSE 0 END, " +
            "great_count = great_count + CASE WHEN :type = 'GREAT' THEN 1 ELSE 0 END, " +
            "amazing_count = amazing_count + CASE WHEN :type = 'AMAZING' THEN 1 ELSE 0 END, " +
            "fighting_count = fighting_count + CASE WHEN :type = 'FIGHTING' THEN 1 ELSE 0 END " +
            "WHERE record_id = :recordId",
            nativeQuery = true)
    int increaseCount(@Param("recordId") Long recordId, @Param("type") String type);

    // ReactionScheduler가 배치를 (recordId, type) 단위로 그룹핑해 합산한 증가량(delta)을
    // 조합당 UPDATE 1회로 반영하기 위한 메서드. 건당 increaseCount(=+1)를 N번 호출하던 것을
    // 조합 수만큼의 increaseCountBy 호출로 줄인다.
    @Modifying
    @Query(value = "UPDATE record_reaction_count " +
            "SET total_count = total_count + :delta, " +
            "awesome_count = awesome_count + CASE WHEN :type = 'AWESOME' THEN :delta ELSE 0 END, " +
            "great_count = great_count + CASE WHEN :type = 'GREAT' THEN :delta ELSE 0 END, " +
            "amazing_count = amazing_count + CASE WHEN :type = 'AMAZING' THEN :delta ELSE 0 END, " +
            "fighting_count = fighting_count + CASE WHEN :type = 'FIGHTING' THEN :delta ELSE 0 END " +
            "WHERE record_id = :recordId",
            nativeQuery = true)
    int increaseCountBy(@Param("recordId") Long recordId, @Param("type") String type, @Param("delta") long delta);

    @Modifying
    @Query(value = "UPDATE record_reaction_count " +
            "SET total_count = total_count - 1, " +
            "awesome_count = awesome_count - CASE WHEN :type = 'AWESOME' THEN 1 ELSE 0 END, " +
            "great_count = great_count - CASE WHEN :type = 'GREAT' THEN 1 ELSE 0 END, " +
            "amazing_count = amazing_count - CASE WHEN :type = 'AMAZING' THEN 1 ELSE 0 END, " +
            "fighting_count = fighting_count - CASE WHEN :type = 'FIGHTING' THEN 1 ELSE 0 END " +
            "WHERE record_id = :recordId",
            nativeQuery = true)
    int decreaseCount(@Param("recordId") Long recordId, @Param("type") String type);
}

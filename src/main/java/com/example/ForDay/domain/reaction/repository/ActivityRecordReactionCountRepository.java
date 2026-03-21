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

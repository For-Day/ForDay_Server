package com.example.ForDay.domain.reaction.repository;

import com.example.ForDay.domain.reaction.entity.ActivityRecordReactionCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityRecordReactionCountRepository extends JpaRepository<ActivityRecordReactionCount, Long> {

    // 예전에는 UPDATE 후 영향받은 행이 0이면 save()로 초기 행을 만드는 방식이었다 -
    // 동시에 여러 요청이 "행이 없음"을 보고 같이 INSERT를 시도하면서 MySQL 데드락이
    // 실측(#374/#375 부하테스트) 중 확인됐다(check-then-insert 경쟁 상태). INSERT ...
    // ON DUPLICATE KEY UPDATE로 단일 원자적 statement로 바꿔 경쟁 상태 자체를 없앤다.
    @Modifying
    @Query(value = "INSERT INTO record_reaction_count " +
            "(record_id, total_count, awesome_count, great_count, amazing_count, fighting_count, created_at, updated_at) " +
            "VALUES (:recordId, 1, " +
            "CASE WHEN :type = 'AWESOME' THEN 1 ELSE 0 END, " +
            "CASE WHEN :type = 'GREAT' THEN 1 ELSE 0 END, " +
            "CASE WHEN :type = 'AMAZING' THEN 1 ELSE 0 END, " +
            "CASE WHEN :type = 'FIGHTING' THEN 1 ELSE 0 END, " +
            "NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE " +
            "total_count = total_count + 1, " +
            "awesome_count = awesome_count + CASE WHEN :type = 'AWESOME' THEN 1 ELSE 0 END, " +
            "great_count = great_count + CASE WHEN :type = 'GREAT' THEN 1 ELSE 0 END, " +
            "amazing_count = amazing_count + CASE WHEN :type = 'AMAZING' THEN 1 ELSE 0 END, " +
            "fighting_count = fighting_count + CASE WHEN :type = 'FIGHTING' THEN 1 ELSE 0 END, " +
            "updated_at = NOW()",
            nativeQuery = true)
    void upsertIncreaseCount(@Param("recordId") Long recordId, @Param("type") String type);

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

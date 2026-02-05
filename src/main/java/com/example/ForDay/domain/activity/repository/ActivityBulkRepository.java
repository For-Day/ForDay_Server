package com.example.ForDay.domain.activity.repository;

import com.example.ForDay.domain.activity.entity.Activity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ActivityBulkRepository {

    private final JdbcTemplate jdbcTemplate;

    public void bulkInsertActivities(List<Activity> activities) {
        String sql = "INSERT INTO user_activities " +
                "(ai_recommended, content, user_hobby_id, user_id, created_at, updated_at, collected_sticker_num) " +
                "VALUES (?, ?, ?, ?, NOW(), NOW(), 0)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Activity activity = activities.get(i);
                ps.setBoolean(1, activity.isAiRecommended());
                ps.setString(2, activity.getContent());
                ps.setLong(3, activity.getHobby().getId());
                ps.setString(4, activity.getUser().getId());
            }

            @Override
            public int getBatchSize() {
                return activities.size();
            }
        });
    }
}
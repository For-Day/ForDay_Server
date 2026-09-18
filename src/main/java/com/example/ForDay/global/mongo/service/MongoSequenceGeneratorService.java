package com.example.ForDay.global.mongo.service;

import com.example.ForDay.global.mongo.document.DatabaseSequence;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * 이슈 #408 - Mongo로 옮긴 알림의 ID를 기존과 동일한 숫자(Long)로 유지하기 위한 시퀀스
 * 발급기. Mongo 기본 {@code ObjectId}(문자열)를 그대로 쓰면 API 응답의
 * {@code notificationId}가 숫자에서 문자열로 바뀌어 구버전 앱이 깨질 수 있어(#406 논의),
 * counters 컬렉션에서 원자적으로 증가시킨 값을 직접 {@code _id}로 부여한다.
 */
@Service
@RequiredArgsConstructor
public class MongoSequenceGeneratorService {

    private final MongoOperations mongoOperations;

    public long generateSequence(String sequenceName) {
        DatabaseSequence counter = mongoOperations.findAndModify(
                Query.query(Criteria.where("_id").is(sequenceName)),
                new Update().inc("seq", 1),
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                DatabaseSequence.class);
        return counter != null ? counter.getSeq() : 1L;
    }
}

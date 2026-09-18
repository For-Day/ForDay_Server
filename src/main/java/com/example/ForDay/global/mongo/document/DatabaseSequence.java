package com.example.ForDay.global.mongo.document;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB에는 RDB의 auto-increment 같은 내장 시퀀스가 없다. {@code counters} 컬렉션에
 * 시퀀스 이름별로 현재 값을 한 문서씩 두고, {@link com.example.ForDay.global.mongo.service.MongoSequenceGeneratorService}가
 * 원자적 증가(findAndModify)로 다음 값을 발급한다 - MongoDB 공식 문서가 제시하는
 * auto-increment 대체 패턴이다.
 */
@Document(collection = "counters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DatabaseSequence {

    @Id
    private String id;

    private long seq;

    public DatabaseSequence(String id, long seq) {
        this.id = id;
        this.seq = seq;
    }
}

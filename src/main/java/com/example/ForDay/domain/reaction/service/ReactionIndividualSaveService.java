package com.example.ForDay.domain.reaction.service;

import com.example.ForDay.domain.reaction.entity.ActivityRecordReaction;
import com.example.ForDay.domain.reaction.repository.ActivityRecordReactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * ReactionScheduler의 벌크 저장(saveAll)이 배치 내 중복 데이터로 실패했을 때,
 * 건별로 재시도해 정상 건만 저장하기 위한 헬퍼.
 * <p>
 * 건별 저장은 반드시 REQUIRES_NEW로 각각 독립된 트랜잭션에서 실행한다.
 * 같은 트랜잭션 안에서 실패한 flush 이후 계속 저장을 시도하면 영속성 컨텍스트가
 * 오염되어 이후 건까지 함께 실패할 수 있으므로, 건별로 트랜잭션을 분리해
 * 한 건의 중복이 다른 건의 저장에 영향을 주지 않도록 한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReactionIndividualSaveService {

    private final ActivityRecordReactionRepository recordReactionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean saveIfNotDuplicate(ActivityRecordReaction reaction) {
        try {
            recordReactionRepository.save(reaction);
            recordReactionRepository.flush();
            return true;
        } catch (DataIntegrityViolationException e) {
            log.warn("[reaction] 중복 반응으로 건별 저장 스킵 - recordId: {}, userId: {}, type: {}",
                    reaction.getActivityRecord().getId(), reaction.getReactedUser().getId(), reaction.getReactionType());
            return false;
        }
    }
}

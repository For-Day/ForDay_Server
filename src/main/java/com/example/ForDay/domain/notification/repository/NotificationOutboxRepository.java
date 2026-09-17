package com.example.ForDay.domain.notification.repository;

import com.example.ForDay.domain.notification.entity.NotificationOutbox;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

    // 릴레이가 이번 주기에 처리할 후보 id 목록만 가볍게 조회한다. 잠금은 여기서 걸지 않고
    // findByIdForUpdate에서 건별로 건다 - 후보 목록을 고르는 동안 락을 오래 들고 있지 않기 위해서다.
    @Query("select o.id from NotificationOutbox o where o.status = com.example.ForDay.domain.notification.type.OutboxStatus.PENDING order by o.id asc")
    List<Long> findPendingIds(Pageable pageable);

    // 비관적 락(FOR UPDATE) - 블루-그린 전환 구간에 두 인스턴스가 동시에 폴링해도
    // 같은 행을 동시에 발행하지 못한다. 한쪽이 잠그면 다른 쪽은 대기했다가, 이미
    // PUBLISHED로 바뀐 걸 확인하고 스킵한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from NotificationOutbox o where o.id = :id")
    Optional<NotificationOutbox> findByIdForUpdate(@Param("id") Long id);
}

package com.bunyad.orderplatform.order.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

    List<OutboxEvent> findByAggregateId(UUID aggregateId);
}

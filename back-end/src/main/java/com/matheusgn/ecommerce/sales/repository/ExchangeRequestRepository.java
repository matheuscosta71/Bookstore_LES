package com.matheusgn.ecommerce.sales.repository;

import com.matheusgn.ecommerce.sales.entity.ExchangeRequest;
import com.matheusgn.ecommerce.sales.entity.ExchangeStatus;
import com.matheusgn.ecommerce.sales.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExchangeRequestRepository extends JpaRepository<ExchangeRequest, UUID>,
        JpaSpecificationExecutor<ExchangeRequest> {

    List<ExchangeRequest> findByStatusOrderByCreatedAtDesc(ExchangeStatus status);

    List<ExchangeRequest> findByOrder_StatusOrderByCreatedAtDesc(OrderStatus orderStatus);

    /** Cupom de troca após recebimento (RF0044) — enriquecer listagem de pedidos do cliente. */
    List<ExchangeRequest> findByOrder_IdInAndStatus(Collection<UUID> orderIds, ExchangeStatus status);

    Optional<ExchangeRequest> findByIdAndOrder_Id(UUID id, UUID orderId);

    boolean existsByOrder_IdAndStatusIn(UUID orderId, Collection<ExchangeStatus> statuses);

    boolean existsByOrderItem_Id(UUID orderItemId);
}

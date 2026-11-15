package pl.piomin.services.delivery.query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliveryViewRepository extends JpaRepository<DeliveryView, UUID> {

    Page<DeliveryView> findByOrderId(UUID orderId, Pageable pageable);

    List<DeliveryView> findByOrderIdOrderByUpdatedAtAsc(UUID orderId);
}

package pl.piomin.services.payment.query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentViewRepository extends JpaRepository<PaymentView, UUID> {

    Page<PaymentView> findByOrderId(UUID orderId, Pageable pageable);

    List<PaymentView> findByOrderIdOrderByUpdatedAtAsc(UUID orderId);
}

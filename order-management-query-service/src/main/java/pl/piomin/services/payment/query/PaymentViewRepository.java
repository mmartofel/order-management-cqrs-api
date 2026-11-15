package pl.piomin.services.payment.query;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface PaymentViewRepository extends JpaRepository<PaymentView, UUID>,
        JpaSpecificationExecutor<PaymentView> {

    List<PaymentView> findByOrderIdOrderByUpdatedAtAsc(UUID orderId);
}

package pl.piomin.services.order.query;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface OrderViewRepository extends JpaRepository<OrderView, UUID>, JpaSpecificationExecutor<OrderView> {
}

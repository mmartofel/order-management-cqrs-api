package pl.piomin.services.order.query;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Read model of an order, written only by the order projection. */
@Entity
@Table(name = "order_view")
public class OrderView {

    @Id
    private UUID id;

    @Column(name = "person_id", nullable = false)
    private Long personId;

    @Column(name = "address_id", nullable = false)
    private Long addressId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "delivery_id")
    private UUID deliveryId;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Sequence number of the last applied event of this order. */
    @Column(nullable = false)
    private long version;

    @ElementCollection
    @CollectionTable(name = "order_line_view", joinColumns = @JoinColumn(name = "order_id"))
    @OrderColumn(name = "line_no")
    private List<OrderLineView> lines = new ArrayList<>();

    protected OrderView() {
    }

    public OrderView(UUID id, Long personId, Long addressId, BigDecimal total, Instant placedAt,
                     List<OrderLineView> lines, long version) {
        this.id = id;
        this.personId = personId;
        this.addressId = addressId;
        this.status = OrderStatus.PLACED;
        this.total = total;
        this.placedAt = placedAt;
        this.updatedAt = placedAt;
        this.lines.addAll(lines);
        this.version = version;
    }

    /** Records a change made by the event with the given sequence number. */
    public void changed(OrderStatus status, Instant at, long version) {
        this.status = status;
        this.updatedAt = at;
        this.version = version;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public void setDeliveryId(UUID deliveryId) {
        this.deliveryId = deliveryId;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public UUID getId() {
        return id;
    }

    public Long getPersonId() {
        return personId;
    }

    public Long getAddressId() {
        return addressId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public UUID getDeliveryId() {
        return deliveryId;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public List<OrderLineView> getLines() {
        return lines;
    }
}

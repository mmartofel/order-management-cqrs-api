package pl.piomin.services.delivery.query;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import pl.piomin.services.delivery.domain.DeliveryStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Read model of a delivery, written only by the delivery projection. */
@Entity
@Table(name = "delivery_view")
public class DeliveryView {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "address_id", nullable = false)
    private Long addressId;

    @Column(nullable = false, length = 100)
    private String carrier;

    @Column(name = "scheduled_for", nullable = false)
    private LocalDate scheduledFor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus status;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Sequence number of the last applied event of this delivery. */
    @Column(nullable = false)
    private long version;

    protected DeliveryView() {
    }

    public DeliveryView(UUID id, UUID orderId, Long addressId, String carrier, LocalDate scheduledFor, Instant at,
                        long version) {
        this.id = id;
        this.orderId = orderId;
        this.addressId = addressId;
        this.carrier = carrier;
        this.scheduledFor = scheduledFor;
        this.status = DeliveryStatus.SCHEDULED;
        this.updatedAt = at;
        this.version = version;
    }

    public void changed(DeliveryStatus status, Instant at, long version) {
        this.status = status;
        this.updatedAt = at;
        this.version = version;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public Long getAddressId() {
        return addressId;
    }

    public String getCarrier() {
        return carrier;
    }

    public LocalDate getScheduledFor() {
        return scheduledFor;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public String getTrackingNumber() {
        return trackingNumber;
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
}

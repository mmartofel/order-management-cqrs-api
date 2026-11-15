package pl.piomin.services.payment.query;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import pl.piomin.services.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Read model of a payment, written only by the payment projection. */
@Entity
@Table(name = "payment_view")
public class PaymentView {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 30)
    private String method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Sequence number of the last applied event of this payment. */
    @Column(nullable = false)
    private long version;

    protected PaymentView() {
    }

    public PaymentView(UUID id, UUID orderId, BigDecimal amount, String method, Instant at, long version) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.method = method;
        this.status = PaymentStatus.INITIATED;
        this.updatedAt = at;
        this.version = version;
    }

    public void changed(PaymentStatus status, Instant at, long version) {
        this.status = status;
        this.updatedAt = at;
        this.version = version;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getMethod() {
        return method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}

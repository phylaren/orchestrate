package genius.project.orchestrate.swap.internal.persistence;

import genius.project.orchestrate.chore.SwapType;
import genius.project.orchestrate.swap.SwapRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "swap_requests")
public class SwapRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "chore_id", nullable = false, updatable = false)
    private UUID choreId;

    @Column(name = "initiator_user_id", nullable = false, updatable = false)
    private UUID initiatorUserId;

    @Column(name = "receiver_user_id", nullable = false, updatable = false)
    private UUID receiverUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SwapRequestStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "swap_type", nullable = false, length = 20)
    private SwapType swapType;

    @Column(name = "cycle_number")
    private Integer cycleNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected SwapRequestEntity() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getChoreId() {
        return choreId;
    }

    public UUID getInitiatorUserId() {
        return initiatorUserId;
    }

    public UUID getReceiverUserId() {
        return receiverUserId;
    }

    public SwapRequestStatus getStatus() {
        return status;
    }

    public SwapType getSwapType() {
        return swapType;
    }

    public Integer getCycleNumber() {
        return cycleNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setChoreId(UUID choreId) {
        this.choreId = choreId;
    }

    public void setInitiatorUserId(UUID initiatorUserId) {
        this.initiatorUserId = initiatorUserId;
    }

    public void setReceiverUserId(UUID receiverUserId) {
        this.receiverUserId = receiverUserId;
    }

    public void setStatus(SwapRequestStatus status) {
        this.status = status;
    }

    public void setSwapType(SwapType swapType) {
        this.swapType = swapType;
    }

    public void setCycleNumber(Integer cycleNumber) {
        this.cycleNumber = cycleNumber;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        SwapRequestEntity that = (SwapRequestEntity) other;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }
}

package genius.project.orchestrate.chore.internal.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rotation_schedules")
public class RotationScheduleEntity {

    @Id
    @Column(name = "chore_id")
    private UUID choreId;

    @ElementCollection
    @CollectionTable(name = "rotation_order", joinColumns = @JoinColumn(name = "chore_id"))
    @OrderColumn(name = "position")
    @Column(name = "user_id", nullable = false)
    private List<UUID> baseOrder = new ArrayList<>();

    @Column(name = "current_index", nullable = false)
    private int currentIndex;

    @Column(name = "current_cycle_number", nullable = false)
    private int currentCycleNumber;

    @Column(name = "cycle_started_at", nullable = false)
    private Instant cycleStartedAt;

    protected RotationScheduleEntity() {}

    public RotationScheduleEntity(UUID choreId, List<UUID> baseOrder, int currentIndex,
                                  int currentCycleNumber, Instant cycleStartedAt) {
        this.choreId = choreId;
        this.baseOrder = baseOrder != null ? new ArrayList<>(baseOrder) : new ArrayList<>();
        this.currentIndex = currentIndex;
        this.currentCycleNumber = currentCycleNumber;
        this.cycleStartedAt = cycleStartedAt;
    }

    public UUID getChoreId() { return choreId; }
    public List<UUID> getBaseOrder() { return baseOrder; }
    public void setBaseOrder(List<UUID> baseOrder) { this.baseOrder = baseOrder; }
    public int getCurrentIndex() { return currentIndex; }
    public void setCurrentIndex(int currentIndex) { this.currentIndex = currentIndex; }
    public int getCurrentCycleNumber() { return currentCycleNumber; }
    public void setCurrentCycleNumber(int currentCycleNumber) { this.currentCycleNumber = currentCycleNumber; }
    public Instant getCycleStartedAt() { return cycleStartedAt; }
    public void setCycleStartedAt(Instant cycleStartedAt) { this.cycleStartedAt = cycleStartedAt; }
}
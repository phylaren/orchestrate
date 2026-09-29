package genius.project.orchestrate.chore.internal.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chores")
public class ChoreEntity {

    @Id
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "recurrence_days", nullable = false)
    private int recurrenceDays;

    @Column(name = "requires_confirmation", nullable = false)
    private boolean requiresConfirmation;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "chore", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChoreParticipantEntity> participants = new ArrayList<>();

    @OneToMany(mappedBy = "chore", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChoreCompletionEntity> completions = new ArrayList<>();

    protected ChoreEntity() {}

    public ChoreEntity(UUID id, UUID householdId, String name, String description,
                       int recurrenceDays, boolean requiresConfirmation, Instant createdAt) {
        this.id = id;
        this.householdId = householdId;
        this.name = name;
        this.description = description;
        this.recurrenceDays = recurrenceDays;
        this.requiresConfirmation = requiresConfirmation;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getHouseholdId() { return householdId; }
    public void setHouseholdId(UUID householdId) { this.householdId = householdId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getRecurrenceDays() { return recurrenceDays; }
    public void setRecurrenceDays(int recurrenceDays) { this.recurrenceDays = recurrenceDays; }
    public boolean isRequiresConfirmation() { return requiresConfirmation; }
    public void setRequiresConfirmation(boolean requiresConfirmation) { this.requiresConfirmation = requiresConfirmation; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public List<ChoreParticipantEntity> getParticipants() { return participants; }
    public List<ChoreCompletionEntity> getCompletions() { return completions; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChoreEntity that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
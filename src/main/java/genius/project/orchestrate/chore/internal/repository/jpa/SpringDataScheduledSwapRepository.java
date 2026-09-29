package genius.project.orchestrate.chore.internal.repository.jpa;

import genius.project.orchestrate.chore.internal.persistence.ScheduledSwapEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataScheduledSwapRepository
        extends JpaRepository<ScheduledSwapEntity, UUID> {

    List<ScheduledSwapEntity> findByChoreIdAndCycleNumber(UUID choreId, int cycleNumber);

    @Modifying
    @Query("DELETE FROM ScheduledSwapEntity s WHERE s.choreId = :choreId AND s.cycleNumber < :currentCycleNumber")
    void deleteExpired(@Param("choreId") UUID choreId, @Param("currentCycleNumber") int currentCycleNumber);
}
package genius.project.orchestrate.swap.internal.persistence;

import genius.project.orchestrate.swap.SwapRequestStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SwapRequestJpaRepository extends ListCrudRepository<SwapRequestEntity, UUID> {

    List<SwapRequestEntity> findByChoreIdOrderByCreatedAtDesc(UUID choreId);

    boolean existsByChoreIdAndInitiatorUserIdAndReceiverUserIdAndStatus(
            UUID choreId,
            UUID initiatorUserId,
            UUID receiverUserId,
            SwapRequestStatus status);

    long countByChoreIdAndStatus(UUID choreId, SwapRequestStatus status);

    @Query("""
            select r from SwapRequestEntity r
            where r.choreId = :choreId
              and r.status = :status
            order by r.createdAt desc
            """)
    List<SwapRequestEntity> findByChoreIdAndStatusOrdered(
            @Param("choreId") UUID choreId,
            @Param("status") SwapRequestStatus status);

    @Query("""
            select distinct r from SwapRequestEntity r
            where r.choreId = :choreId
              and (r.initiatorUserId = :userId or r.receiverUserId = :userId)
            order by r.createdAt desc
            """)
    List<SwapRequestEntity> findInvolvingUser(
            @Param("choreId") UUID choreId,
            @Param("userId") UUID userId);

    @Query(value = """
            select status as status, count(*) as total
            from swap_requests
            where chore_id = :choreId
            group by status
            order by status
            """, nativeQuery = true)
    List<StatusCount> countByStatusNative(@Param("choreId") UUID choreId);

    interface StatusCount {

        String getStatus();

        long getTotal();
    }
}

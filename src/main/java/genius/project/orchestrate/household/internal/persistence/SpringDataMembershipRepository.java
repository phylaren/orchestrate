package genius.project.orchestrate.household.internal.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataMembershipRepository extends JpaRepository<MembershipEntity, MembershipId> {

    @Query("""
            select m from MembershipEntity m
            left join fetch m.permissions
            where m.id.householdId = :householdId and m.id.userId = :userId
            """)
    Optional<MembershipEntity> findWithPermissions(@Param("householdId") UUID householdId,
                                                   @Param("userId") UUID userId);

    @Query("""
            select distinct m from MembershipEntity m
            left join fetch m.permissions
            where m.id.householdId = :householdId
            order by m.joinedAt
            """)
    List<MembershipEntity> findAllByHouseholdIdWithPermissions(@Param("householdId") UUID householdId);

    @Query("""
            select distinct m from MembershipEntity m
            left join fetch m.permissions
            where m.id.userId = :userId
            order by m.joinedAt
            """)
    List<MembershipEntity> findAllByUserIdWithPermissions(@Param("userId") UUID userId);
}

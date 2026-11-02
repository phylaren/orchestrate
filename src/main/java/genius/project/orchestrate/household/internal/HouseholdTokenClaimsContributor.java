package genius.project.orchestrate.household.internal;

import genius.project.orchestrate.identity.TokenClaimsContributor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
class HouseholdTokenClaimsContributor implements TokenClaimsContributor {

    private static final int MAX_HOUSEHOLDS_IN_TOKEN = 50;

    private final MembershipRepository membershipRepository;

    HouseholdTokenClaimsContributor(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Override
    public Map<String, Object> claimsFor(UUID userId) {
        Map<String, List<String>> households = membershipRepository.findByUserId(userId).stream()
                .limit(MAX_HOUSEHOLDS_IN_TOKEN)
                .collect(Collectors.toMap(
                        membership -> membership.householdId().toString(),
                        membership -> membership.permissions().stream()
                                .map(Enum::name)
                                .toList()
                ));

        return Map.of("households", households);
    }
}
package genius.project.orchestrate.household.dto;

import genius.project.orchestrate.household.MembershipPermission;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record PermissionsUpdateRequest(
        @NotNull(message = "permissions is required")
        Set<@NotNull(message = "permissions must not contain null") MembershipPermission> permissions
) {
}

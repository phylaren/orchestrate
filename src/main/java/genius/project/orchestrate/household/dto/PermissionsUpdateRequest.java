package genius.project.orchestrate.household.dto;

import genius.project.orchestrate.household.MembershipPermission;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

@Schema(description = "Новий повний набір адміністративних прав учасника (замінює попередній)")
public record PermissionsUpdateRequest(
        @Schema(description = "Права учасника; порожній набір позбавляє всіх адміністративних прав",
                example = "[\"MANAGE_CHORES\", \"CONFIRM_COMPLETIONS\"]")
        @NotNull(message = "permissions is required")
        Set<@NotNull(message = "permissions must not contain null") MembershipPermission> permissions
) {
}

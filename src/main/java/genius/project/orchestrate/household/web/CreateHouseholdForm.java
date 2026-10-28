package genius.project.orchestrate.household.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateHouseholdForm(

        @NotBlank(message = "Назва домогосподарства є обов’язковою")
        @Size(max = 120, message = "Назва має містити не більше 120 символів")
        String name
) {

    public static CreateHouseholdForm empty() {
        return new CreateHouseholdForm("");
    }
}

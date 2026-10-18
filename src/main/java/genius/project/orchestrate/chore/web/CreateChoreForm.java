package genius.project.orchestrate.chore.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateChoreForm(

        @NotBlank(message = "Назва обов’язку є обов’язковою")
        @Size(max = 120, message = "Назва має містити не більше 120 символів")
        String name,

        @Size(max = 1000, message = "Опис має містити не більше 1000 символів")
        String description,

        @NotBlank(message = "Вкажіть інтервал повторення в днях")
        @Pattern(regexp = CreateChoreForm.RECURRENCE_DAYS_REGEXP,
                message = "Інтервал має бути цілим числом від 1 до 365")
        String recurrenceDays,

        Boolean requiresConfirmation
) {

    public CreateChoreForm {
        requiresConfirmation = Boolean.TRUE.equals(requiresConfirmation);
    }

    static final String RECURRENCE_DAYS_REGEXP = "^([1-9]|[1-9]\\d|[12]\\d\\d|3[0-5]\\d|36[0-5])$";

    public static CreateChoreForm empty() {
        return new CreateChoreForm("", "", "7", false);
    }

    public int recurrenceDaysValue() {
        return Integer.parseInt(recurrenceDays);
    }
}
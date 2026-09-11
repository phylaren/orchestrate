package genius.project.orchestrate.chore.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@NotNull(message = "recurrenceDays is required")
@Min(value = 1, message = "recurrenceDays must be at least 1")
@Max(value = 365, message = "recurrenceDays must be at most 365")
@Constraint(validatedBy = {})
public @interface ValidRecurrenceDays {

    String message() default "recurrenceDays must be between 1 and 365";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

package genius.project.orchestrate.chore.web;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CreateChoreFormValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static Set<String> invalidFields(CreateChoreForm form) {
        return validator.validate(form).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("коректна форма не має порушень")
    void validForm_HasNoViolations() {
        CreateChoreForm form = new CreateChoreForm("Винести сміття", "До 20:00", "3", true);

        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    @DisplayName("порожня форма за замовчуванням (empty) проходить валідацію лише після введення назви")
    void emptyForm_FailsOnlyOnName() {
        assertThat(invalidFields(CreateChoreForm.empty())).containsExactly("name");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("порожня або пробільна назва — помилка поля name")
    void blankName_IsRejected(String name) {
        assertThat(invalidFields(new CreateChoreForm(name, null, "7", false))).containsExactly("name");
    }

    @Test
    @DisplayName("назва довша за 120 символів відхиляється, рівно 120 — приймається")
    void nameLength_BoundaryIsChecked() {
        assertThat(invalidFields(new CreateChoreForm("а".repeat(121), null, "7", false))).containsExactly("name");
        assertThat(validator.validate(new CreateChoreForm("а".repeat(120), null, "7", false))).isEmpty();
    }

    @Test
    @DisplayName("опис довший за 1000 символів відхиляється, опис може бути відсутнім")
    void descriptionLength_BoundaryIsChecked() {
        assertThat(invalidFields(new CreateChoreForm("Прибирання", "а".repeat(1001), "7", false)))
                .containsExactly("description");
        assertThat(validator.validate(new CreateChoreForm("Прибирання", null, "7", false))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "9", "10", "99", "100", "299", "300", "359", "360", "365"})
    @DisplayName("інтервал 1–365 приймається")
    void recurrenceDays_InRange_IsAccepted(String value) {
        assertThat(validator.validate(new CreateChoreForm("Прибирання", null, value, false))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "366", "999", "1000", "-1", "+5", "007", "3.5", "abc", "1 0", " 7"})
    @DisplayName("інтервал поза 1–365 або не ціле число відхиляється")
    void recurrenceDays_OutOfRangeOrMalformed_IsRejected(String value) {
        assertThat(invalidFields(new CreateChoreForm("Прибирання", null, value, false)))
                .containsExactly("recurrenceDays");
    }

    @Test
    @DisplayName("відсутній інтервал (null) дає помилку NotBlank")
    void recurrenceDays_Null_IsRejected() {
        assertThat(invalidFields(new CreateChoreForm("Прибирання", null, null, false)))
                .containsExactly("recurrenceDays");
    }

    @Test
    @DisplayName("recurrenceDaysValue() перетворює валідований рядок на число")
    void recurrenceDaysValue_ParsesString() {
        assertThat(new CreateChoreForm("Прибирання", null, "365", false).recurrenceDaysValue()).isEqualTo(365);
    }
}

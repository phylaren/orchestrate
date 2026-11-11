package genius.project.orchestrate.audit.internal;

import genius.project.orchestrate.audit.AuditAction;
import genius.project.orchestrate.identity.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditLoggingAspectTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000f1");

    private final AuditLogWriter writer = mock(AuditLogWriter.class);
    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private Target target;

    @BeforeEach
    void setUp() {
        when(currentUserProvider.getUserId()).thenReturn(USER_ID);
        AspectJProxyFactory factory = new AspectJProxyFactory(new Target());
        factory.addAspect(new AuditLoggingAspect(writer, currentUserProvider));
        target = factory.getProxy();
    }

    @Test
    @DisplayName("успіх: id, користувач, метод, аргументи, результат і час зафіксовані")
    void success_RecordsAllData() {
        String result = target.work("abc", 3);

        assertThat(result).isEqualTo("abc3");
        AuditLogEntity entry = captured();
        assertThat(entry.getId()).isNotNull();
        assertThat(entry.getUserId()).isEqualTo(USER_ID);
        assertThat(entry.getAction()).isEqualTo("WORK");
        assertThat(entry.getMethodName()).isEqualTo("Target.work");
        assertThat(entry.getArguments()).isEqualTo("name=abc, n=3");
        assertThat(entry.getResult()).isEqualTo("abc3");
        assertThat(entry.isSuccess()).isTrue();
        assertThat(entry.getErrorType()).isNull();
        assertThat(entry.getOccurredAt()).isNotNull();
        assertThat(entry.getDurationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("виняток прокидається далі, запис містить тип; для logParameters=false без аргументів і тексту")
    void failure_RethrowsAndRecordsWithoutSecrets() {
        assertThatThrownBy(() -> target.fail("s3cret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("bad s3cret");

        AuditLogEntity entry = captured();
        assertThat(entry.isSuccess()).isFalse();
        assertThat(entry.getErrorType()).isEqualTo(IllegalArgumentException.class.getName());
        assertThat(entry.getArguments()).isNull();
        assertThat(entry.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("довгий текст помилки та результат обрізаються до меж колонок")
    void longValues_AreTruncatedToColumnLimits() {
        assertThatThrownBy(() -> target.failLong()).isInstanceOf(IllegalStateException.class);

        AuditLogEntity entry = captured();
        assertThat(entry.getErrorMessage()).hasSize(AuditLogEntity.ERROR_MESSAGE_LENGTH).endsWith("...");
    }

    @Test
    @DisplayName("без автентифікованого користувача запис робиться з user_id = null")
    void anonymous_RecordsNullUser() {
        when(currentUserProvider.getUserId()).thenThrow(new IllegalStateException("No authenticated user"));

        target.work("a", 1);

        assertThat(captured().getUserId()).isNull();
    }

    @Test
    @DisplayName("збій запису аудиту не ламає бізнес-операцію")
    void writerFailure_DoesNotBreakBusinessMethod() {
        doThrow(new IllegalStateException("db down")).when(writer).save(any());

        assertThat(target.work("a", 1)).isEqualTo("a1");
    }

    private AuditLogEntity captured() {
        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(writer).save(captor.capture());
        return captor.getValue();
    }

    public static class Target {

        @AuditAction(action = "WORK")
        public String work(String name, int n) {
            return name + n;
        }

        @AuditAction(action = "FAIL", logParameters = false)
        public void fail(String secret) {
            throw new IllegalArgumentException("bad " + secret);
        }

        @AuditAction(action = "FAIL_LONG")
        public void failLong() {
            throw new IllegalStateException("x".repeat(AuditLogEntity.ERROR_MESSAGE_LENGTH + 500));
        }
    }
}
package genius.project.orchestrate.audit.internal;

import genius.project.orchestrate.audit.AuditAction;
import genius.project.orchestrate.audit.AuditLogService;
import genius.project.orchestrate.audit.AuditRecord;
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

    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private Target target;

    @BeforeEach
    void setUp() {
        when(currentUserProvider.getUserId()).thenReturn(USER_ID);
        AspectJProxyFactory factory = new AspectJProxyFactory(new Target());
        factory.addAspect(new AuditLoggingAspect(auditLogService, currentUserProvider));
        target = factory.getProxy();
    }

    @Test
    @DisplayName("успіх: користувач, метод, аргументи, результат і час зафіксовані")
    void success_RecordsAllData() {
        String result = target.work("abc", 3);

        assertThat(result).isEqualTo("abc3");
        AuditRecord record = captured();
        assertThat(record.userId()).isEqualTo(USER_ID);
        assertThat(record.action()).isEqualTo("WORK");
        assertThat(record.methodName()).isEqualTo("Target.work");
        assertThat(record.arguments()).isEqualTo("name=abc, n=3");
        assertThat(record.result()).isEqualTo("abc3");
        assertThat(record.success()).isTrue();
        assertThat(record.errorType()).isNull();
        assertThat(record.occurredAt()).isNotNull();
        assertThat(record.durationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("виняток прокидається далі, запис містить тип; для logParameters=false без аргументів і тексту")
    void failure_RethrowsAndRecordsWithoutSecrets() {
        assertThatThrownBy(() -> target.fail("s3cret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("bad s3cret");

        AuditRecord record = captured();
        assertThat(record.success()).isFalse();
        assertThat(record.errorType()).isEqualTo(IllegalArgumentException.class.getName());
        assertThat(record.arguments()).isNull();
        assertThat(record.errorMessage()).isNull();
    }

    @Test
    @DisplayName("без автентифікованого користувача запис робиться з user_id = null")
    void anonymous_RecordsNullUser() {
        when(currentUserProvider.getUserId()).thenThrow(new IllegalStateException("No authenticated user"));
        target.work("a", 1);
        assertThat(captured().userId()).isNull();
    }

    @Test
    @DisplayName("збій запису аудиту не ламає бізнес-операцію")
    void serviceFailure_DoesNotBreakBusinessMethod() {
        doThrow(new IllegalStateException("db down")).when(auditLogService).record(any());
        assertThat(target.work("a", 1)).isEqualTo("a1");
    }

    private AuditRecord captured() {
        ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditLogService).record(captor.capture());
        return captor.getValue();
    }

    public static class Target {
        @AuditAction(action = "WORK")
        public String work(String name, int n) { return name + n; }

        @AuditAction(action = "FAIL", logParameters = false)
        public void fail(String secret) { throw new IllegalArgumentException("bad " + secret); }
    }
}

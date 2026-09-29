package genius.project.orchestrate.swap.internal;

import genius.project.orchestrate.chore.SwapType;
import genius.project.orchestrate.chore.client.ChoreClient;
import genius.project.orchestrate.identity.CurrentUserProvider;
import genius.project.orchestrate.swap.SwapRequestStatus;
import genius.project.orchestrate.swap.dto.SwapRequestRequest;
import genius.project.orchestrate.swap.dto.SwapRequestResponse;
import genius.project.orchestrate.swap.dto.SwapRequestStatusRequest;
import genius.project.orchestrate.swap.dto.SwapRequestSummaryResponse;
import genius.project.orchestrate.swap.dto.SwapRequestUpdateRequest;
import genius.project.orchestrate.swap.exception.DuplicateSwapRequestException;
import genius.project.orchestrate.swap.exception.SwapRequestNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
@Sql(scripts = "/db/test-seed/referenced-aggregates.sql")
class SwapRequestServicePersistenceTest {

    private static final UUID CHORE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID INITIATOR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID RECEIVER = UUID.fromString("00000000-0000-0000-0000-0000000000a2");

    @Autowired
    private SwapRequestRepository repository;

    @Autowired
    private SwapRequestResponseMapper responseMapper;

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final ChoreClient choreClient = mock(ChoreClient.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private SwapRequestServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SwapRequestServiceImpl(
                repository, currentUserProvider, choreClient, eventPublisher, responseMapper);
    }

    @Test
    @DisplayName("create writes a pending row")
    void createWritesRow() {
        givenCurrentUser(INITIATOR);
        givenParticipants();

        SwapRequestResponse created = service.createSwapRequest(CHORE_ID,
                new SwapRequestRequest(RECEIVER, SwapType.PERMANENT, null));

        assertThat(created.id()).isNotNull();
        assertThat(repository.findById(created.id()))
                .hasValueSatisfying(saved -> {
                    assertThat(saved.status()).isEqualTo(SwapRequestStatus.PENDING);
                    assertThat(saved.swapType()).isEqualTo(SwapType.PERMANENT);
                    assertThat(saved.cycleNumber()).isNull();
                });
    }

    @Test
    @DisplayName("the duplicate guard works against stored rows, not an in-memory map")
    void duplicateGuardReadsDatabase() {
        givenCurrentUser(INITIATOR);
        givenParticipants();

        service.createSwapRequest(CHORE_ID, new SwapRequestRequest(RECEIVER, SwapType.PERMANENT, null));

        assertThatThrownBy(() -> service.createSwapRequest(CHORE_ID,
                new SwapRequestRequest(RECEIVER, SwapType.PERMANENT, null)))
                .isInstanceOf(DuplicateSwapRequestException.class);
    }

    @Test
    @DisplayName("update replaces the mutable terms of a pending row")
    void updateReplacesTerms() {
        givenCurrentUser(INITIATOR);
        givenParticipants();
        when(choreClient.currentCycleNumber(CHORE_ID)).thenReturn(1);

        SwapRequestResponse created = service.createSwapRequest(CHORE_ID,
                new SwapRequestRequest(RECEIVER, SwapType.PERMANENT, null));

        service.updateSwapRequest(CHORE_ID, created.id(),
                new SwapRequestUpdateRequest(SwapType.TEMPORARY, 4));

        assertThat(repository.findById(created.id()))
                .hasValueSatisfying(updated -> {
                    assertThat(updated.swapType()).isEqualTo(SwapType.TEMPORARY);
                    assertThat(updated.cycleNumber()).isEqualTo(4);
                    assertThat(updated.createdAt()).isEqualTo(created.createdAt());
                });
    }

    @Test
    @DisplayName("respond persists the target status")
    void respondPersistsStatus() {
        givenCurrentUser(INITIATOR);
        givenParticipants();
        SwapRequestResponse created = service.createSwapRequest(CHORE_ID,
                new SwapRequestRequest(RECEIVER, SwapType.PERMANENT, null));

        givenCurrentUser(RECEIVER);
        service.respondToSwapRequest(CHORE_ID, created.id(),
                new SwapRequestStatusRequest(SwapRequestStatus.ACCEPTED));

        assertThat(repository.findById(created.id()))
                .hasValueSatisfying(resolved -> assertThat(resolved.status()).isEqualTo(SwapRequestStatus.ACCEPTED));
    }

    @Test
    @DisplayName("delete removes the row")
    void deleteRemovesRow() {
        givenCurrentUser(INITIATOR);
        givenParticipants();
        SwapRequestResponse created = service.createSwapRequest(CHORE_ID,
                new SwapRequestRequest(RECEIVER, SwapType.PERMANENT, null));

        service.deleteSwapRequest(CHORE_ID, created.id());

        assertThat(repository.findById(created.id())).isEmpty();
        assertThatThrownBy(() -> service.getSwapRequest(CHORE_ID, created.id()))
                .isInstanceOf(SwapRequestNotFoundException.class);
    }

    @Test
    @DisplayName("summary aggregates the stored rows")
    void summaryAggregatesRows() {
        givenCurrentUser(INITIATOR);
        givenParticipants();
        SwapRequestResponse first = service.createSwapRequest(CHORE_ID,
                new SwapRequestRequest(RECEIVER, SwapType.PERMANENT, null));

        givenCurrentUser(RECEIVER);
        service.respondToSwapRequest(CHORE_ID, first.id(),
                new SwapRequestStatusRequest(SwapRequestStatus.REJECTED));

        givenCurrentUser(INITIATOR);
        service.createSwapRequest(CHORE_ID, new SwapRequestRequest(RECEIVER, SwapType.TEMPORARY, 2));

        SwapRequestSummaryResponse summary = service.getSummary(CHORE_ID);

        assertThat(summary.total()).isEqualTo(2);
        assertThat(summary.byStatus())
                .containsEntry(SwapRequestStatus.PENDING, 1L)
                .containsEntry(SwapRequestStatus.REJECTED, 1L);
    }

    private void givenCurrentUser(UUID userId) {
        when(currentUserProvider.getUserId()).thenReturn(userId);
    }

    private void givenParticipants() {
        when(choreClient.isParticipant(any(), any())).thenReturn(true);
    }
}

package genius.project.orchestrate.swap.internal;

import genius.project.orchestrate.chore.SwapType;
import genius.project.orchestrate.chore.TurnSwapRequestedEvent;
import genius.project.orchestrate.chore.client.ChoreClient;
import genius.project.orchestrate.common.exception.InvalidCycleNumberException;
import genius.project.orchestrate.identity.CurrentUserProvider;
import genius.project.orchestrate.swap.SwapRequestStatus;
import genius.project.orchestrate.swap.dto.SwapRequestRequest;
import genius.project.orchestrate.swap.dto.SwapRequestResponse;
import genius.project.orchestrate.swap.dto.SwapRequestStatusRequest;
import genius.project.orchestrate.swap.dto.SwapRequestSummaryResponse;
import genius.project.orchestrate.swap.dto.SwapRequestUpdateRequest;
import genius.project.orchestrate.swap.exception.DuplicateSwapRequestException;
import genius.project.orchestrate.swap.exception.InvalidSwapRequestRecipientException;
import genius.project.orchestrate.swap.exception.InvalidSwapRequestStatusException;
import genius.project.orchestrate.swap.exception.NotChoreParticipantException;
import genius.project.orchestrate.swap.exception.NotInSameGroupException;
import genius.project.orchestrate.swap.exception.NotSwapRequestInitiatorException;
import genius.project.orchestrate.swap.exception.NotSwapRequestReceiverException;
import genius.project.orchestrate.swap.exception.SwapRequestNotEditableException;
import genius.project.orchestrate.swap.exception.SwapRequestNotFoundException;
import genius.project.orchestrate.swap.internal.domain.SwapRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SwapRequestServiceImplTest {

    @Mock private SwapRequestRepository repository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private ChoreClient choreClient;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private SwapRequestResponseMapper responseMapper;

    @InjectMocks
    private SwapRequestServiceImpl service;

    private static final UUID CHORE_ID   = UUID.randomUUID();
    private static final UUID REQUEST_ID = UUID.randomUUID();
    private static final UUID INITIATOR  = UUID.randomUUID();
    private static final UUID RECEIVER   = UUID.randomUUID();
    private static final LocalDateTime NOW = LocalDateTime.now();

    @BeforeEach
    void stubResponseMapper() {
        lenient().when(responseMapper.toResponse(any(SwapRequest.class))).thenAnswer(invocation -> {
            SwapRequest request = invocation.getArgument(0);
            return new SwapRequestResponse(
                    request.id(),
                    request.choreId(),
                    request.initiatorUserId(),
                    request.receiverUserId(),
                    request.status(),
                    request.swapType(),
                    request.cycleNumber(),
                    request.createdAt());
        });
    }

    @Nested
    @DisplayName("createSwapRequest")
    class Create {

        @Test
        @DisplayName("PERMANENT: happy path")
        void permanent_HappyPath() {
            SwapRequest saved = swapRequest(SwapRequestStatus.PENDING, SwapType.PERMANENT, null);
            when(currentUserProvider.getUserId()).thenReturn(INITIATOR);
            when(choreClient.isParticipant(CHORE_ID, INITIATOR)).thenReturn(true);
            when(choreClient.isParticipant(CHORE_ID, RECEIVER)).thenReturn(true);
            when(repository.existsPending(CHORE_ID, INITIATOR, RECEIVER)).thenReturn(false);
            when(repository.save(any())).thenReturn(saved);

            SwapRequestResponse result = service.createSwapRequest(CHORE_ID,
                    new SwapRequestRequest(RECEIVER, SwapType.PERMANENT, null));

            assertThat(result.swapType()).isEqualTo(SwapType.PERMANENT);
            assertThat(result.cycleNumber()).isNull();
        }

        @Test
        @DisplayName("TEMPORARY: stores cycleNumber")
        void temporary_StoresCycle() {
            SwapRequest saved = swapRequest(SwapRequestStatus.PENDING, SwapType.TEMPORARY, 5);
            when(currentUserProvider.getUserId()).thenReturn(INITIATOR);
            when(choreClient.isParticipant(CHORE_ID, INITIATOR)).thenReturn(true);
            when(choreClient.isParticipant(CHORE_ID, RECEIVER)).thenReturn(true);
            when(repository.existsPending(CHORE_ID, INITIATOR, RECEIVER)).thenReturn(false);
            when(choreClient.currentCycleNumber(CHORE_ID)).thenReturn(3);
            when(repository.save(any())).thenReturn(saved);

            SwapRequestResponse result = service.createSwapRequest(CHORE_ID,
                    new SwapRequestRequest(RECEIVER, SwapType.TEMPORARY, 5));

            assertThat(result.swapType()).isEqualTo(SwapType.TEMPORARY);
            assertThat(result.cycleNumber()).isEqualTo(5);
        }

        @Test
        @DisplayName("TEMPORARY without cycleNumber: throws")
        void temporary_MissingCycle() {
            when(currentUserProvider.getUserId()).thenReturn(INITIATOR);
            when(choreClient.isParticipant(CHORE_ID, INITIATOR)).thenReturn(true);
            when(choreClient.isParticipant(CHORE_ID, RECEIVER)).thenReturn(true);
            when(repository.existsPending(CHORE_ID, INITIATOR, RECEIVER)).thenReturn(false);

            assertThatThrownBy(() -> service.createSwapRequest(CHORE_ID,
                    new SwapRequestRequest(RECEIVER, SwapType.TEMPORARY, null)))
                    .isInstanceOf(InvalidCycleNumberException.class)
                    .extracting("errorCode").isEqualTo("MISSING_CYCLE_NUMBER");
        }

        @Test
        @DisplayName("TEMPORARY in past: throws")
        void temporary_PastCycle() {
            when(currentUserProvider.getUserId()).thenReturn(INITIATOR);
            when(choreClient.isParticipant(CHORE_ID, INITIATOR)).thenReturn(true);
            when(choreClient.isParticipant(CHORE_ID, RECEIVER)).thenReturn(true);
            when(repository.existsPending(CHORE_ID, INITIATOR, RECEIVER)).thenReturn(false);
            when(choreClient.currentCycleNumber(CHORE_ID)).thenReturn(10);

            assertThatThrownBy(() -> service.createSwapRequest(CHORE_ID,
                    new SwapRequestRequest(RECEIVER, SwapType.TEMPORARY, 5)))
                    .isInstanceOf(InvalidCycleNumberException.class)
                    .extracting("errorCode").isEqualTo("CYCLE_IN_PAST");
        }

        @Test
        @DisplayName("TEMPORARY on current cycle: throws")
        void temporary_CurrentCycle() {
            when(currentUserProvider.getUserId()).thenReturn(INITIATOR);
            when(choreClient.isParticipant(CHORE_ID, INITIATOR)).thenReturn(true);
            when(choreClient.isParticipant(CHORE_ID, RECEIVER)).thenReturn(true);
            when(repository.existsPending(CHORE_ID, INITIATOR, RECEIVER)).thenReturn(false);
            when(choreClient.currentCycleNumber(CHORE_ID)).thenReturn(5);

            assertThatThrownBy(() -> service.createSwapRequest(CHORE_ID,
                    new SwapRequestRequest(RECEIVER, SwapType.TEMPORARY, 5)))
                    .isInstanceOf(InvalidCycleNumberException.class)
                    .extracting("errorCode").isEqualTo("CYCLE_IN_PAST");
        }

        @Test
        @DisplayName("TEMPORARY too far: throws")
        void temporary_TooFar() {
            when(currentUserProvider.getUserId()).thenReturn(INITIATOR);
            when(choreClient.isParticipant(CHORE_ID, INITIATOR)).thenReturn(true);
            when(choreClient.isParticipant(CHORE_ID, RECEIVER)).thenReturn(true);
            when(repository.existsPending(CHORE_ID, INITIATOR, RECEIVER)).thenReturn(false);
            when(choreClient.currentCycleNumber(CHORE_ID)).thenReturn(3);

            assertThatThrownBy(() -> service.createSwapRequest(CHORE_ID,
                    new SwapRequestRequest(RECEIVER, SwapType.TEMPORARY, 100)))
                    .isInstanceOf(InvalidCycleNumberException.class)
                    .extracting("errorCode").isEqualTo("CYCLE_TOO_FAR");
        }

        @Test
        @DisplayName("initiator equals receiver: throws")
        void initiatorEqualsReceiver() {
            when(currentUserProvider.getUserId()).thenReturn(INITIATOR);

            assertThatThrownBy(() -> service.createSwapRequest(CHORE_ID,
                    new SwapRequestRequest(INITIATOR, SwapType.PERMANENT, null)))
                    .isInstanceOf(InvalidSwapRequestRecipientException.class);
        }

        @Test
        @DisplayName("initiator not participant: throws")
        void initiatorNotParticipant() {
            when(currentUserProvider.getUserId()).thenReturn(INITIATOR);
            when(choreClient.isParticipant(CHORE_ID, INITIATOR)).thenReturn(false);

            assertThatThrownBy(() -> service.createSwapRequest(CHORE_ID,
                    new SwapRequestRequest(RECEIVER, SwapType.PERMANENT, null)))
                    .isInstanceOf(NotChoreParticipantException.class);
        }

        @Test
        @DisplayName("receiver not participant: throws")
        void receiverNotParticipant() {
            when(currentUserProvider.getUserId()).thenReturn(INITIATOR);
            when(choreClient.isParticipant(CHORE_ID, INITIATOR)).thenReturn(true);
            when(choreClient.isParticipant(CHORE_ID, RECEIVER)).thenReturn(false);

            assertThatThrownBy(() -> service.createSwapRequest(CHORE_ID,
                    new SwapRequestRequest(RECEIVER, SwapType.PERMANENT, null)))
                    .isInstanceOf(NotChoreParticipantException.class);
        }

        @Test
        @DisplayName("duplicate pending: throws")
        void duplicate() {
            when(currentUserProvider.getUserId()).thenReturn(INITIATOR);
            when(choreClient.isParticipant(CHORE_ID, INITIATOR)).thenReturn(true);
            when(choreClient.isParticipant(CHORE_ID, RECEIVER)).thenReturn(true);
            when(repository.existsPending(CHORE_ID, INITIATOR, RECEIVER)).thenReturn(true);

            assertThatThrownBy(() -> service.createSwapRequest(CHORE_ID,
                    new SwapRequestRequest(RECEIVER, SwapType.PERMANENT, null)))
                    .isInstanceOf(DuplicateSwapRequestException.class);
        }
    }

    @Nested
    @DisplayName("respondToSwapRequest")
    class Respond {

        @Test
        @DisplayName("PERMANENT accepted: publishes event with null cycleNumber")
        void permanentAccepted_Publishes() {
            SwapRequest pending = swapRequest(SwapRequestStatus.PENDING, SwapType.PERMANENT, null);
            SwapRequest accepted = swapRequest(SwapRequestStatus.ACCEPTED, SwapType.PERMANENT, null);
            when(currentUserProvider.getUserId()).thenReturn(RECEIVER);
            when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pending));
            when(choreClient.isParticipant(CHORE_ID, INITIATOR)).thenReturn(true);
            when(choreClient.isParticipant(CHORE_ID, RECEIVER)).thenReturn(true);
            when(repository.update(any())).thenReturn(accepted);

            service.respondToSwapRequest(CHORE_ID, REQUEST_ID,
                    new SwapRequestStatusRequest(SwapRequestStatus.ACCEPTED));

            ArgumentCaptor<TurnSwapRequestedEvent> captor =
                    ArgumentCaptor.forClass(TurnSwapRequestedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().swapType()).isEqualTo(SwapType.PERMANENT);
            assertThat(captor.getValue().cycleNumber()).isNull();
        }

        @Test
        @DisplayName("TEMPORARY accepted: publishes event with cycleNumber")
        void temporaryAccepted_PublishesWithCycle() {
            SwapRequest pending = swapRequest(SwapRequestStatus.PENDING, SwapType.TEMPORARY, 5);
            SwapRequest accepted = swapRequest(SwapRequestStatus.ACCEPTED, SwapType.TEMPORARY, 5);
            when(currentUserProvider.getUserId()).thenReturn(RECEIVER);
            when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pending));
            when(choreClient.isParticipant(CHORE_ID, INITIATOR)).thenReturn(true);
            when(choreClient.isParticipant(CHORE_ID, RECEIVER)).thenReturn(true);
            when(repository.update(any())).thenReturn(accepted);

            service.respondToSwapRequest(CHORE_ID, REQUEST_ID,
                    new SwapRequestStatusRequest(SwapRequestStatus.ACCEPTED));

            ArgumentCaptor<TurnSwapRequestedEvent> captor =
                    ArgumentCaptor.forClass(TurnSwapRequestedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().cycleNumber()).isEqualTo(5);
        }

        @Test
        @DisplayName("accept when user left group: throws")
        void accept_UserLeft() {
            SwapRequest pending = swapRequest(SwapRequestStatus.PENDING, SwapType.PERMANENT, null);
            when(currentUserProvider.getUserId()).thenReturn(RECEIVER);
            when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pending));
            when(choreClient.isParticipant(CHORE_ID, INITIATOR)).thenReturn(false);

            assertThatThrownBy(() -> service.respondToSwapRequest(CHORE_ID, REQUEST_ID,
                    new SwapRequestStatusRequest(SwapRequestStatus.ACCEPTED)))
                    .isInstanceOf(NotInSameGroupException.class)
                    .extracting("errorCode").isEqualTo("NOT_IN_SAME_GROUP");

            verify(repository, never()).update(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("REJECTED: no event")
        void rejected_NoEvent() {
            SwapRequest pending = swapRequest(SwapRequestStatus.PENDING, SwapType.PERMANENT, null);
            SwapRequest rejected = swapRequest(SwapRequestStatus.REJECTED, SwapType.PERMANENT, null);
            when(currentUserProvider.getUserId()).thenReturn(RECEIVER);
            when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pending));
            when(repository.update(any())).thenReturn(rejected);

            service.respondToSwapRequest(CHORE_ID, REQUEST_ID,
                    new SwapRequestStatusRequest(SwapRequestStatus.REJECTED));

            verify(eventPublisher, never()).publishEvent(any());
            verify(choreClient, never()).isParticipant(any(), any());
        }

        @Test
        @DisplayName("request not found: throws")
        void notFound() {
            when(currentUserProvider.getUserId()).thenReturn(RECEIVER);
            when(repository.findById(REQUEST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.respondToSwapRequest(CHORE_ID, REQUEST_ID,
                    new SwapRequestStatusRequest(SwapRequestStatus.ACCEPTED)))
                    .isInstanceOf(SwapRequestNotFoundException.class);
        }

        @Test
        @DisplayName("wrong chore: throws")
        void wrongChore() {
            SwapRequest pending = swapRequest(SwapRequestStatus.PENDING, SwapType.PERMANENT, null);
            when(currentUserProvider.getUserId()).thenReturn(RECEIVER);
            when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pending));

            assertThatThrownBy(() -> service.respondToSwapRequest(UUID.randomUUID(), REQUEST_ID,
                    new SwapRequestStatusRequest(SwapRequestStatus.ACCEPTED)))
                    .isInstanceOf(SwapRequestNotFoundException.class);
        }

        @Test
        @DisplayName("not receiver: throws")
        void notReceiver() {
            SwapRequest pending = swapRequest(SwapRequestStatus.PENDING, SwapType.PERMANENT, null);
            when(currentUserProvider.getUserId()).thenReturn(INITIATOR);
            when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pending));

            assertThatThrownBy(() -> service.respondToSwapRequest(CHORE_ID, REQUEST_ID,
                    new SwapRequestStatusRequest(SwapRequestStatus.ACCEPTED)))
                    .isInstanceOf(NotSwapRequestReceiverException.class);
        }

        @Test
        @DisplayName("invalid transition: throws")
        void invalidTransition() {
            SwapRequest accepted = swapRequest(SwapRequestStatus.ACCEPTED, SwapType.PERMANENT, null);
            when(currentUserProvider.getUserId()).thenReturn(RECEIVER);
            when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(accepted));

            assertThatThrownBy(() -> service.respondToSwapRequest(CHORE_ID, REQUEST_ID,
                    new SwapRequestStatusRequest(SwapRequestStatus.REJECTED)))
                    .isInstanceOf(InvalidSwapRequestStatusException.class);
        }
    }

    @Nested
    @DisplayName("getSwapRequests")
    class Get {

        @Test
        @DisplayName("returns mapped list")
        void returnsMapped() {
            SwapRequest stored = swapRequest(SwapRequestStatus.PENDING, SwapType.PERMANENT, null);
            when(repository.findByChoreId(CHORE_ID)).thenReturn(List.of(stored));

            List<SwapRequestResponse> result = service.getSwapRequests(CHORE_ID);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().swapType()).isEqualTo(SwapType.PERMANENT);
        }
    }

    @Nested
    @DisplayName("getSwapRequest")
    class GetOne {

        @Test
        @DisplayName("returns the request of this chore")
        void returnsRequest() {
            when(repository.findById(REQUEST_ID))
                    .thenReturn(Optional.of(swapRequest(SwapRequestStatus.PENDING, SwapType.PERMANENT, null)));

            SwapRequestResponse result = service.getSwapRequest(CHORE_ID, REQUEST_ID);

            assertThat(result.id()).isEqualTo(REQUEST_ID);
        }

        @Test
        @DisplayName("not found: throws")
        void notFound() {
            when(repository.findById(REQUEST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getSwapRequest(CHORE_ID, REQUEST_ID))
                    .isInstanceOf(SwapRequestNotFoundException.class);
        }

        @Test
        @DisplayName("belongs to another chore: throws")
        void otherChore() {
            when(repository.findById(REQUEST_ID))
                    .thenReturn(Optional.of(swapRequest(SwapRequestStatus.PENDING, SwapType.PERMANENT, null)));

            assertThatThrownBy(() -> service.getSwapRequest(UUID.randomUUID(), REQUEST_ID))
                    .isInstanceOf(SwapRequestNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getSwapRequests with status filter")
    class GetFiltered {

        @Test
        @DisplayName("delegates to the status query")
        void delegatesToStatusQuery() {
            when(repository.findByChoreIdAndStatus(CHORE_ID, SwapRequestStatus.PENDING))
                    .thenReturn(List.of(swapRequest(SwapRequestStatus.PENDING, SwapType.PERMANENT, null)));

            assertThat(service.getSwapRequests(CHORE_ID, SwapRequestStatus.PENDING)).hasSize(1);
        }

        @Test
        @DisplayName("null status falls back to the full list")
        void nullStatusFallsBack() {
            when(repository.findByChoreId(CHORE_ID))
                    .thenReturn(List.of(swapRequest(SwapRequestStatus.ACCEPTED, SwapType.PERMANENT, null)));

            assertThat(service.getSwapRequests(CHORE_ID, null)).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getSummary")
    class Summary {

        @Test
        @DisplayName("sums the per-status counters")
        void sumsCounters() {
            when(repository.countGroupedByStatus(CHORE_ID)).thenReturn(Map.of(
                    SwapRequestStatus.PENDING, 2L,
                    SwapRequestStatus.ACCEPTED, 1L,
                    SwapRequestStatus.REJECTED, 0L));

            SwapRequestSummaryResponse summary = service.getSummary(CHORE_ID);

            assertThat(summary.choreId()).isEqualTo(CHORE_ID);
            assertThat(summary.total()).isEqualTo(3);
            assertThat(summary.byStatus()).containsEntry(SwapRequestStatus.PENDING, 2L);
        }
    }

    @Nested
    @DisplayName("updateSwapRequest")
    class Update {

        @Test
        @DisplayName("PERMANENT to TEMPORARY: stores the validated cycle")
        void updatesTerms() {
            when(currentUserProvider.getUserId()).thenReturn(INITIATOR);
            when(repository.findById(REQUEST_ID))
                    .thenReturn(Optional.of(swapRequest(SwapRequestStatus.PENDING, SwapType.PERMANENT, null)));
            when(choreClient.currentCycleNumber(CHORE_ID)).thenReturn(2);
            when(repository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

            SwapRequestResponse result = service.updateSwapRequest(CHORE_ID, REQUEST_ID,
                    new SwapRequestUpdateRequest(SwapType.TEMPORARY, 5));

            assertThat(result.swapType()).isEqualTo(SwapType.TEMPORARY);
            assertThat(result.cycleNumber()).isEqualTo(5);
            assertThat(result.status()).isEqualTo(SwapRequestStatus.PENDING);
        }

        @Test
        @DisplayName("not the initiator: throws")
        void notInitiator() {
            when(currentUserProvider.getUserId()).thenReturn(RECEIVER);
            when(repository.findById(REQUEST_ID))
                    .thenReturn(Optional.of(swapRequest(SwapRequestStatus.PENDING, SwapType.PERMANENT, null)));

            assertThatThrownBy(() -> service.updateSwapRequest(CHORE_ID, REQUEST_ID,
                    new SwapRequestUpdateRequest(SwapType.PERMANENT, null)))
                    .isInstanceOf(NotSwapRequestInitiatorException.class)
                    .extracting("errorCode").isEqualTo("NOT_SWAP_REQUEST_INITIATOR");

            verify(repository, never()).update(any());
        }

        @Test
        @DisplayName("already resolved: throws")
        void notEditable() {
            when(currentUserProvider.getUserId()).thenReturn(INITIATOR);
            when(repository.findById(REQUEST_ID))
                    .thenReturn(Optional.of(swapRequest(SwapRequestStatus.ACCEPTED, SwapType.PERMANENT, null)));

            assertThatThrownBy(() -> service.updateSwapRequest(CHORE_ID, REQUEST_ID,
                    new SwapRequestUpdateRequest(SwapType.PERMANENT, null)))
                    .isInstanceOf(SwapRequestNotEditableException.class)
                    .extracting("errorCode").isEqualTo("SWAP_REQUEST_NOT_EDITABLE");

            verify(repository, never()).update(any());
        }

        @Test
        @DisplayName("TEMPORARY without cycleNumber: throws")
        void missingCycle() {
            when(currentUserProvider.getUserId()).thenReturn(INITIATOR);
            when(repository.findById(REQUEST_ID))
                    .thenReturn(Optional.of(swapRequest(SwapRequestStatus.PENDING, SwapType.PERMANENT, null)));

            assertThatThrownBy(() -> service.updateSwapRequest(CHORE_ID, REQUEST_ID,
                    new SwapRequestUpdateRequest(SwapType.TEMPORARY, null)))
                    .isInstanceOf(InvalidCycleNumberException.class)
                    .extracting("errorCode").isEqualTo("MISSING_CYCLE_NUMBER");
        }

        @Test
        @DisplayName("request of another chore: throws")
        void otherChore() {
            when(currentUserProvider.getUserId()).thenReturn(INITIATOR);
            when(repository.findById(REQUEST_ID))
                    .thenReturn(Optional.of(swapRequest(SwapRequestStatus.PENDING, SwapType.PERMANENT, null)));

            assertThatThrownBy(() -> service.updateSwapRequest(UUID.randomUUID(), REQUEST_ID,
                    new SwapRequestUpdateRequest(SwapType.PERMANENT, null)))
                    .isInstanceOf(SwapRequestNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteSwapRequest")
    class Delete {

        @Test
        @DisplayName("initiator deletes a pending request")
        void deletes() {
            when(currentUserProvider.getUserId()).thenReturn(INITIATOR);
            when(repository.findById(REQUEST_ID))
                    .thenReturn(Optional.of(swapRequest(SwapRequestStatus.PENDING, SwapType.PERMANENT, null)));

            service.deleteSwapRequest(CHORE_ID, REQUEST_ID);

            verify(repository).deleteById(REQUEST_ID);
        }

        @Test
        @DisplayName("not the initiator: throws")
        void notInitiator() {
            when(currentUserProvider.getUserId()).thenReturn(RECEIVER);
            when(repository.findById(REQUEST_ID))
                    .thenReturn(Optional.of(swapRequest(SwapRequestStatus.PENDING, SwapType.PERMANENT, null)));

            assertThatThrownBy(() -> service.deleteSwapRequest(CHORE_ID, REQUEST_ID))
                    .isInstanceOf(NotSwapRequestInitiatorException.class);

            verify(repository, never()).deleteById(any());
        }

        @Test
        @DisplayName("already resolved: throws")
        void notEditable() {
            when(currentUserProvider.getUserId()).thenReturn(INITIATOR);
            when(repository.findById(REQUEST_ID))
                    .thenReturn(Optional.of(swapRequest(SwapRequestStatus.REJECTED, SwapType.PERMANENT, null)));

            assertThatThrownBy(() -> service.deleteSwapRequest(CHORE_ID, REQUEST_ID))
                    .isInstanceOf(SwapRequestNotEditableException.class);

            verify(repository, never()).deleteById(any());
        }
    }

    private SwapRequest swapRequest(SwapRequestStatus status, SwapType type, Integer cycle) {
        return new SwapRequest(REQUEST_ID, CHORE_ID, INITIATOR, RECEIVER, status, type, cycle, NOW);
    }
}
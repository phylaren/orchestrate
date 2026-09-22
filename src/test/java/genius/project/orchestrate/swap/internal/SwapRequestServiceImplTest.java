package genius.project.orchestrate.swap.internal;

import genius.project.orchestrate.chore.SwapType;
import genius.project.orchestrate.chore.TurnSwapRequestedEvent;
import genius.project.orchestrate.chore.client.ChoreClient;
import genius.project.orchestrate.common.exception.BusinessRuleViolationException;
import genius.project.orchestrate.identity.CurrentUserProvider;
import genius.project.orchestrate.swap.SwapRequestStatus;
import genius.project.orchestrate.swap.dto.SwapRequestRequest;
import genius.project.orchestrate.swap.dto.SwapRequestResponse;
import genius.project.orchestrate.swap.dto.SwapRequestStatusRequest;
import genius.project.orchestrate.swap.exception.DuplicateSwapRequestException;
import genius.project.orchestrate.swap.exception.InvalidSwapRequestRecipientException;
import genius.project.orchestrate.swap.exception.InvalidSwapRequestStatusException;
import genius.project.orchestrate.swap.exception.NotChoreParticipantException;
import genius.project.orchestrate.swap.exception.NotSwapRequestReceiverException;
import genius.project.orchestrate.swap.exception.SwapRequestNotFoundException;
import genius.project.orchestrate.swap.internal.domain.SwapRequest;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SwapRequestServiceImplTest {

    @Mock private SwapRequestRepository repository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private ChoreClient choreClient;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SwapRequestServiceImpl service;

    private static final UUID CHORE_ID   = UUID.randomUUID();
    private static final UUID REQUEST_ID = UUID.randomUUID();
    private static final UUID INITIATOR  = UUID.randomUUID();
    private static final UUID RECEIVER   = UUID.randomUUID();
    private static final LocalDateTime NOW = LocalDateTime.now();

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
                    .isInstanceOf(BusinessRuleViolationException.class)
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
                    .isInstanceOf(BusinessRuleViolationException.class)
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
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("errorCode").isEqualTo("CYCLE_TOO_FAR");
        }

        @Test
        @DisplayName("INSERT: rejected")
        void insert_Rejected() {
            assertThatThrownBy(() -> service.createSwapRequest(CHORE_ID,
                    new SwapRequestRequest(RECEIVER, SwapType.INSERT, null)))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("errorCode").isEqualTo("INSERT_NOT_SWAP_REQUEST");
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
                    .isInstanceOf(BusinessRuleViolationException.class)
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
            assertThat(result.get(0).swapType()).isEqualTo(SwapType.PERMANENT);
        }
    }

    private SwapRequest swapRequest(SwapRequestStatus status, SwapType type, Integer cycle) {
        return new SwapRequest(REQUEST_ID, CHORE_ID, INITIATOR, RECEIVER, status, type, cycle, NOW);
    }
}
package genius.project.orchestrate.swap.internal;

import genius.project.orchestrate.chore.TurnSwapRequestedEvent;
import genius.project.orchestrate.chore.client.ChoreClient;
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

    @Mock
    private SwapRequestRepository repository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private ChoreClient choreClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SwapRequestServiceImpl service;

    private static final UUID CHORE_ID    = UUID.randomUUID();
    private static final UUID REQUEST_ID  = UUID.randomUUID();
    private static final UUID INITIATOR   = UUID.randomUUID();
    private static final UUID RECEIVER    = UUID.randomUUID();
    private static final LocalDateTime NOW = LocalDateTime.now();

    // -------------------------------------------------------------------------
    // getSwapRequests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getSwapRequests")
    class GetSwapRequests {

        @Test
        @DisplayName("returns mapped list from repository")
        void returnsMappedList() {
            SwapRequest stored = swapRequest(REQUEST_ID, INITIATOR, RECEIVER, SwapRequestStatus.PENDING);
            when(repository.findByChoreId(CHORE_ID)).thenReturn(List.of(stored));

            List<SwapRequestResponse> result = service.getSwapRequests(CHORE_ID);

            verify(repository).findByChoreId(CHORE_ID);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(REQUEST_ID);
            assertThat(result.get(0).choreId()).isEqualTo(CHORE_ID);
            assertThat(result.get(0).initiatorUserId()).isEqualTo(INITIATOR);
            assertThat(result.get(0).receiverUserId()).isEqualTo(RECEIVER);
            assertThat(result.get(0).status()).isEqualTo(SwapRequestStatus.PENDING);
        }

        @Test
        @DisplayName("returns empty list when no requests exist")
        void returnsEmptyList() {
            when(repository.findByChoreId(CHORE_ID)).thenReturn(List.of());

            List<SwapRequestResponse> result = service.getSwapRequests(CHORE_ID);

            verify(repository).findByChoreId(CHORE_ID);
            assertThat(result).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // createSwapRequest
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("createSwapRequest")
    class CreateSwapRequest {

        @Test
        @DisplayName("happy path: saves and returns response")
        void happyPath_SavesAndReturns() {
            SwapRequest saved = swapRequest(REQUEST_ID, INITIATOR, RECEIVER, SwapRequestStatus.PENDING);
            when(currentUserProvider.getUserId()).thenReturn(INITIATOR);
            when(choreClient.isParticipant(CHORE_ID, INITIATOR)).thenReturn(true);
            when(choreClient.isParticipant(CHORE_ID, RECEIVER)).thenReturn(true);
            when(repository.existsPending(CHORE_ID, INITIATOR, RECEIVER)).thenReturn(false);
            when(repository.save(any())).thenReturn(saved);

            SwapRequestResponse result = service.createSwapRequest(CHORE_ID, new SwapRequestRequest(RECEIVER));

            verify(repository).save(any());
            assertThat(result.id()).isEqualTo(REQUEST_ID);
            assertThat(result.status()).isEqualTo(SwapRequestStatus.PENDING);
            assertThat(result.initiatorUserId()).isEqualTo(INITIATOR);
            assertThat(result.receiverUserId()).isEqualTo(RECEIVER);
        }

        @Test
        @DisplayName("initiator equals receiver throws InvalidSwapRequestRecipientException")
        void initiatorEqualsReceiver_ThrowsInvalidRecipient() {
            when(currentUserProvider.getUserId()).thenReturn(INITIATOR);

            assertThatThrownBy(() -> service.createSwapRequest(CHORE_ID, new SwapRequestRequest(INITIATOR)))
                    .isInstanceOf(InvalidSwapRequestRecipientException.class)
                    .extracting("errorCode").isEqualTo("INVALID_SWAP_REQUEST_RECIPIENT");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("initiator is not chore participant throws NotChoreParticipantException")
        void initiatorNotParticipant_Throws() {
            when(currentUserProvider.getUserId()).thenReturn(INITIATOR);
            when(choreClient.isParticipant(CHORE_ID, INITIATOR)).thenReturn(false);

            assertThatThrownBy(() -> service.createSwapRequest(CHORE_ID, new SwapRequestRequest(RECEIVER)))
                    .isInstanceOf(NotChoreParticipantException.class)
                    .extracting("errorCode").isEqualTo("NOT_CHORE_PARTICIPANT");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("receiver is not chore participant throws NotChoreParticipantException")
        void receiverNotParticipant_Throws() {
            when(currentUserProvider.getUserId()).thenReturn(INITIATOR);
            when(choreClient.isParticipant(CHORE_ID, INITIATOR)).thenReturn(true);
            when(choreClient.isParticipant(CHORE_ID, RECEIVER)).thenReturn(false);

            assertThatThrownBy(() -> service.createSwapRequest(CHORE_ID, new SwapRequestRequest(RECEIVER)))
                    .isInstanceOf(NotChoreParticipantException.class)
                    .extracting("errorCode").isEqualTo("NOT_CHORE_PARTICIPANT");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("duplicate pending request throws DuplicateSwapRequestException")
        void duplicatePending_Throws() {
            when(currentUserProvider.getUserId()).thenReturn(INITIATOR);
            when(choreClient.isParticipant(CHORE_ID, INITIATOR)).thenReturn(true);
            when(choreClient.isParticipant(CHORE_ID, RECEIVER)).thenReturn(true);
            when(repository.existsPending(CHORE_ID, INITIATOR, RECEIVER)).thenReturn(true);

            assertThatThrownBy(() -> service.createSwapRequest(CHORE_ID, new SwapRequestRequest(RECEIVER)))
                    .isInstanceOf(DuplicateSwapRequestException.class)
                    .extracting("errorCode").isEqualTo("SWAP_REQUEST_ALREADY_EXISTS");

            verify(repository, never()).save(any());
        }
    }

    // -------------------------------------------------------------------------
    // respondToSwapRequest
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("respondToSwapRequest")
    class RespondToSwapRequest {

        @Test
        @DisplayName("ACCEPTED: saves updated status and publishes TurnSwapRequestedEvent")
        void accepted_SavesAndPublishesEvent() {
            SwapRequest pending = swapRequest(REQUEST_ID, INITIATOR, RECEIVER, SwapRequestStatus.PENDING);
            SwapRequest accepted = swapRequest(REQUEST_ID, INITIATOR, RECEIVER, SwapRequestStatus.ACCEPTED);
            when(currentUserProvider.getUserId()).thenReturn(RECEIVER);
            when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pending));
            when(repository.update(any())).thenReturn(accepted);

            service.respondToSwapRequest(CHORE_ID, REQUEST_ID, new SwapRequestStatusRequest(SwapRequestStatus.ACCEPTED));

            verify(repository).update(any());

            ArgumentCaptor<TurnSwapRequestedEvent> captor = ArgumentCaptor.forClass(TurnSwapRequestedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().choreId()).isEqualTo(CHORE_ID);
            assertThat(captor.getValue().fromUserId()).isEqualTo(INITIATOR);
            assertThat(captor.getValue().toUserId()).isEqualTo(RECEIVER);
        }

        @Test
        @DisplayName("REJECTED: saves updated status and does not publish event")
        void rejected_SavesAndDoesNotPublishEvent() {
            SwapRequest pending = swapRequest(REQUEST_ID, INITIATOR, RECEIVER, SwapRequestStatus.PENDING);
            SwapRequest rejected = swapRequest(REQUEST_ID, INITIATOR, RECEIVER, SwapRequestStatus.REJECTED);
            when(currentUserProvider.getUserId()).thenReturn(RECEIVER);
            when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pending));
            when(repository.update(any())).thenReturn(rejected);

            SwapRequestResponse result = service.respondToSwapRequest(
                    CHORE_ID, REQUEST_ID, new SwapRequestStatusRequest(SwapRequestStatus.REJECTED));

            verify(repository).update(any());
            assertThat(result.status()).isEqualTo(SwapRequestStatus.REJECTED);
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("request not found throws SwapRequestNotFoundException")
        void requestNotFound_Throws() {
            when(currentUserProvider.getUserId()).thenReturn(RECEIVER);
            when(repository.findById(REQUEST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.respondToSwapRequest(
                    CHORE_ID, REQUEST_ID, new SwapRequestStatusRequest(SwapRequestStatus.ACCEPTED)))
                    .isInstanceOf(SwapRequestNotFoundException.class)
                    .extracting("errorCode").isEqualTo("SWAP_REQUEST_NOT_FOUND");

            verify(repository, never()).update(any());
        }

        @Test
        @DisplayName("request belongs to different chore throws SwapRequestNotFoundException")
        void requestBelongsToDifferentChore_Throws() {
            UUID otherChore = UUID.randomUUID();
            SwapRequest pending = swapRequest(REQUEST_ID, INITIATOR, RECEIVER, SwapRequestStatus.PENDING);
            when(currentUserProvider.getUserId()).thenReturn(RECEIVER);
            when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pending));

            assertThatThrownBy(() -> service.respondToSwapRequest(
                    otherChore, REQUEST_ID, new SwapRequestStatusRequest(SwapRequestStatus.ACCEPTED)))
                    .isInstanceOf(SwapRequestNotFoundException.class)
                    .extracting("errorCode").isEqualTo("SWAP_REQUEST_NOT_FOUND");

            verify(repository, never()).update(any());
        }

        @Test
        @DisplayName("current user is not receiver throws NotSwapRequestReceiverException")
        void currentUserIsNotReceiver_Throws() {
            SwapRequest pending = swapRequest(REQUEST_ID, INITIATOR, RECEIVER, SwapRequestStatus.PENDING);
            when(currentUserProvider.getUserId()).thenReturn(INITIATOR);
            when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pending));

            assertThatThrownBy(() -> service.respondToSwapRequest(
                    CHORE_ID, REQUEST_ID, new SwapRequestStatusRequest(SwapRequestStatus.ACCEPTED)))
                    .isInstanceOf(NotSwapRequestReceiverException.class)
                    .extracting("errorCode").isEqualTo("NOT_SWAP_REQUEST_RECEIVER");

            verify(repository, never()).update(any());
        }

        @Test
        @DisplayName("request already resolved throws InvalidSwapRequestStatusException")
        void alreadyResolved_Throws() {
            SwapRequest accepted = swapRequest(REQUEST_ID, INITIATOR, RECEIVER, SwapRequestStatus.ACCEPTED);
            when(currentUserProvider.getUserId()).thenReturn(RECEIVER);
            when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(accepted));

            assertThatThrownBy(() -> service.respondToSwapRequest(
                    CHORE_ID, REQUEST_ID, new SwapRequestStatusRequest(SwapRequestStatus.REJECTED)))
                    .isInstanceOf(InvalidSwapRequestStatusException.class)
                    .extracting("errorCode").isEqualTo("INVALID_SWAP_REQUEST_STATUS");

            verify(repository, never()).update(any());
        }

        @Test
        @DisplayName("setting status back to PENDING throws InvalidSwapRequestStatusException")
        void setBackToPending_Throws() {
            SwapRequest pending = swapRequest(REQUEST_ID, INITIATOR, RECEIVER, SwapRequestStatus.PENDING);
            when(currentUserProvider.getUserId()).thenReturn(RECEIVER);
            when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pending));

            assertThatThrownBy(() -> service.respondToSwapRequest(
                    CHORE_ID, REQUEST_ID, new SwapRequestStatusRequest(SwapRequestStatus.PENDING)))
                    .isInstanceOf(InvalidSwapRequestStatusException.class)
                    .extracting("errorCode").isEqualTo("INVALID_SWAP_REQUEST_STATUS");

            verify(repository, never()).update(any());
        }
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private SwapRequest swapRequest(UUID id, UUID initiator, UUID receiver, SwapRequestStatus status) {
        return new SwapRequest(id, CHORE_ID, initiator, receiver, status, NOW);
    }
}
package genius.project.orchestrate.chore.internal;

import genius.project.orchestrate.chore.TurnSwapRequestedEvent;
import genius.project.orchestrate.chore.client.ChoreClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TurnSwapRequestedEventListenerTest {

    @Mock
    private ChoreClient choreClient;

    @InjectMocks
    private TurnSwapRequestedEventListener listener;

    @Test
    @DisplayName("on TurnSwapRequestedEvent: delegates to choreClient.swapTurns with correct args")
    void onEvent_DelegatesToChoreClient() {
        UUID choreId = UUID.randomUUID();
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();

        listener.on(new TurnSwapRequestedEvent(choreId, from, to));

        verify(choreClient).swapTurns(choreId, from, to);
    }
}
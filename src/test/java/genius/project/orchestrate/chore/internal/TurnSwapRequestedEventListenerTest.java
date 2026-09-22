package genius.project.orchestrate.chore.internal;

import genius.project.orchestrate.chore.RotationService;
import genius.project.orchestrate.chore.SwapType;
import genius.project.orchestrate.chore.TurnSwapRequestedEvent;
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
    private RotationService rotationService;

    @InjectMocks
    private TurnSwapRequestedEventListener listener;

    @Test
    @DisplayName("PERMANENT: delegates to rotationService.swap without cycleNumber")
    void permanent() {
        UUID choreId = UUID.randomUUID();
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();

        listener.on(new TurnSwapRequestedEvent(choreId, from, to, SwapType.PERMANENT, null));

        verify(rotationService).swap(choreId, from, to, SwapType.PERMANENT, null);
    }

    @Test
    @DisplayName("TEMPORARY: delegates with cycleNumber")
    void temporary() {
        UUID choreId = UUID.randomUUID();
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();

        listener.on(new TurnSwapRequestedEvent(choreId, from, to, SwapType.TEMPORARY, 7));

        verify(rotationService).swap(choreId, from, to, SwapType.TEMPORARY, 7);
    }
}
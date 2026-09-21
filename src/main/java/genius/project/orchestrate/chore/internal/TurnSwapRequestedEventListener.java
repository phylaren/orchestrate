package genius.project.orchestrate.chore.internal;

import genius.project.orchestrate.chore.RotationService;
import genius.project.orchestrate.chore.TurnSwapRequestedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class TurnSwapRequestedEventListener {

    private final RotationService rotationService;

    TurnSwapRequestedEventListener(RotationService rotationService) {
        this.rotationService = rotationService;
    }

    @ApplicationModuleListener
    void on(TurnSwapRequestedEvent event) {
        rotationService.swap(
                event.choreId(),
                event.fromUserId(),
                event.toUserId(),
                event.swapType(),
                event.cycleNumber());
    }
}
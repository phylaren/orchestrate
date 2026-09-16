package genius.project.orchestrate.chore.internal;

import genius.project.orchestrate.chore.TurnSwapRequestedEvent;
import genius.project.orchestrate.chore.client.ChoreClient;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class TurnSwapRequestedEventListener {

    private final ChoreClient choreClient;

    TurnSwapRequestedEventListener(ChoreClient choreClient) {
        this.choreClient = choreClient;
    }

    @ApplicationModuleListener
    void on(TurnSwapRequestedEvent event) {
        choreClient.swapTurns(event.choreId(), event.fromUserId(), event.toUserId());
    }
}
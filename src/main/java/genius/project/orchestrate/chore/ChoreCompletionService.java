package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.dto.CompletionResponse;

import java.util.List;
import java.util.UUID;

public interface ChoreCompletionService {

    CompletionResponse markCompleted(UUID choreId, UUID userId);

    List<CompletionResponse> listCompletions(UUID choreId);

    CompletionResponse getCompletion(UUID choreId, UUID completionId);

    CompletionResponse decideConfirmation(UUID choreId, UUID completionId,
                                          UUID confirmedByUserId, boolean approved);
}

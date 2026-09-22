package genius.project.orchestrate.chore;

import genius.project.orchestrate.chore.dto.ChoreResponse;

import java.util.List;
import java.util.UUID;

public interface ChoreLifecycleService {

    ChoreResponse createChore(UUID householdId, String name, String description,
                              int recurrenceDays, boolean requiresConfirmation);

    List<ChoreResponse> listChores(UUID householdId);

    ChoreResponse getChore(UUID choreId);
}

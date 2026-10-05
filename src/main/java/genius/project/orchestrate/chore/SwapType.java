package genius.project.orchestrate.chore;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        Тип обміну чергою: PERMANENT — учасники назавжди міняються місцями в ротації; \
        TEMPORARY — обмін діє лише на вказаний цикл.""")
public enum SwapType {
    PERMANENT,
    TEMPORARY
}
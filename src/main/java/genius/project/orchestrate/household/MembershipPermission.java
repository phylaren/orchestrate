package genius.project.orchestrate.household;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.EnumSet;
import java.util.Set;

@Schema(description = """
        Гранульоване адміністративне право на рівні домогосподарства: \
        MANAGE_CHORES — створювати обов'язки та призначати виконавців; \
        CONFIRM_COMPLETIONS — підтверджувати виконання; \
        INVITE_MEMBERS — керувати кодом запрошення; \
        MANAGE_MEMBERS — видаляти учасників та змінювати їхні права.""")
public enum MembershipPermission {
    MANAGE_CHORES,
    CONFIRM_COMPLETIONS,
    INVITE_MEMBERS,
    MANAGE_MEMBERS;

    public static Set<MembershipPermission> all() {
        return EnumSet.allOf(MembershipPermission.class);
    }

    public static Set<MembershipPermission> defaultForNewMember() {
        return EnumSet.of(CONFIRM_COMPLETIONS);
    }
}

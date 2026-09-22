package genius.project.orchestrate.household;

import java.util.EnumSet;
import java.util.Set;

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

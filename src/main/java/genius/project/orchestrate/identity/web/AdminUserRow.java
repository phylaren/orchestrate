package genius.project.orchestrate.identity.web;

import genius.project.orchestrate.user.UserRole;

record AdminUserRow(String displayName, String email, UserRole role) {
}

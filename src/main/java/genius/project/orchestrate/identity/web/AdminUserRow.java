package genius.project.orchestrate.identity.web;

import genius.project.orchestrate.identity.UserRole;

record AdminUserRow(String displayName, String email, UserRole role) {
}

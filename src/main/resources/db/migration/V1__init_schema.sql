-- Orchestrate — V1__init_schema.sql
--
-- 1. USERS

CREATE TABLE users (
	id UUID NOT NULL,
	display_name VARCHAR(100) NOT NULL,
	email VARCHAR(254) NOT NULL,
	created_at TIMESTAMP WITH TIME ZONE NOT NULL,

	CONSTRAINT pk_users PRIMARY KEY (id),
	CONSTRAINT uq_users_email UNIQUE (email)
);

-- 2. HOUSEHOLDS

CREATE TABLE households (
	id UUID NOT NULL,
	name VARCHAR(120) NOT NULL,
	owner_id UUID NOT NULL,
	created_at TIMESTAMP WITH TIME ZONE NOT NULL,

	CONSTRAINT pk_households PRIMARY KEY (id),
	-- owner_id → users: власник має бути зареєстрованим користувачем.
	-- ON DELETE RESTRICT: не можна видалити користувача, якщо він власник дому.
	CONSTRAINT fk_households_owner FOREIGN KEY (owner_id)
		REFERENCES users (id)
		ON DELETE RESTRICT
);

-- 3. MEMBERSHIPS
-- Один рядок = один учасник одного дому.

CREATE TABLE memberships (
	household_id UUID NOT NULL,
	user_id UUID NOT NULL,
	joined_at TIMESTAMP WITH TIME ZONE NOT NULL,

	CONSTRAINT pk_memberships PRIMARY KEY (household_id, user_id),
	CONSTRAINT fk_memberships_household FOREIGN KEY (household_id)
		REFERENCES households (id)
		ON DELETE CASCADE,
	CONSTRAINT fk_memberships_user FOREIGN KEY (user_id)
		REFERENCES users (id)
		ON DELETE CASCADE
);

-- 4. MEMBERSHIP_PERMISSIONS
-- Set<MembershipPermission> розкладається в окрему таблицю.
-- Значення: MANAGE_CHORES, CONFIRM_COMPLETIONS,
-- INVITE_MEMBERS, MANAGE_MEMBERS

CREATE TABLE membership_permissions (
	household_id UUID NOT NULL,
	user_id UUID NOT NULL,
	permission VARCHAR(50) NOT NULL,

	CONSTRAINT pk_membership_permissions
		PRIMARY KEY (household_id, user_id, permission),
	CONSTRAINT fk_membership_permissions_membership
		FOREIGN KEY (household_id, user_id)
		REFERENCES memberships (household_id, user_id)
		ON DELETE CASCADE
);

-- 5. INVITATION_CODES
-- Один активний код на дім (унікальність по household_id).

CREATE TABLE invitation_codes (
	code VARCHAR(32) NOT NULL,
	household_id UUID NOT NULL,
	created_by_user_id UUID NOT NULL,
	created_at TIMESTAMP WITH TIME ZONE NOT NULL,
	expires_at TIMESTAMP WITH TIME ZONE NOT NULL,

	CONSTRAINT pk_invitation_codes PRIMARY KEY (code),
	-- Один активний код на дім: якщо створюється новий — старий замінюється.
	CONSTRAINT uq_invitation_codes_household UNIQUE (household_id),
	CONSTRAINT fk_invitation_codes_household FOREIGN KEY (household_id)
		REFERENCES households (id)
		ON DELETE CASCADE,
	CONSTRAINT fk_invitation_codes_creator FOREIGN KEY (created_by_user_id)
		REFERENCES users (id)
		ON DELETE RESTRICT
);

-- 6. CHORES

CREATE TABLE chores (
	id UUID NOT NULL,
	household_id UUID NOT NULL,
	name VARCHAR(120) NOT NULL,
	description VARCHAR(1000),
	recurrence_days INT NOT NULL,
	requires_confirmation BOOLEAN NOT NULL,
	created_at TIMESTAMP WITH TIME ZONE NOT NULL,

	CONSTRAINT pk_chores PRIMARY KEY (id),
	CONSTRAINT chk_chores_recurrence CHECK (recurrence_days BETWEEN 1 AND 365),
	CONSTRAINT fk_chores_household FOREIGN KEY (household_id)
		REFERENCES households (id)
		ON DELETE CASCADE
);

-- 7. CHORE_PARTICIPANTS
-- ChoreParticipant: участь користувача в групі ротації обов'язку.

CREATE TABLE chore_participants (
	chore_id UUID NOT NULL,
	user_id UUID NOT NULL,
	joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
	added_by_admin BOOLEAN NOT NULL DEFAULT FALSE,

	CONSTRAINT pk_chore_participants PRIMARY KEY (chore_id, user_id),
	CONSTRAINT fk_chore_participants_chore FOREIGN KEY (chore_id)
		REFERENCES chores (id)
		ON DELETE CASCADE,
	CONSTRAINT fk_chore_participants_user FOREIGN KEY (user_id)
		REFERENCES users (id)
		ON DELETE CASCADE
);

-- 8. ROTATION_SCHEDULES
-- RotationSchedule: поточний стан ротації для обов'язку.
-- Один рядок на обов'язок.

CREATE TABLE rotation_schedules (
	chore_id UUID NOT NULL,
	current_index INT NOT NULL DEFAULT 0,
	current_cycle_number INT NOT NULL DEFAULT 0,
	cycle_started_at TIMESTAMP WITH TIME ZONE NOT NULL,

	CONSTRAINT pk_rotation_schedules PRIMARY KEY (chore_id),
	CONSTRAINT chk_rotation_schedules_index
		CHECK (current_index >= 0),
	CONSTRAINT chk_rotation_schedules_cycle
		CHECK (current_cycle_number >= 0),
	CONSTRAINT fk_rotation_schedules_chore FOREIGN KEY (chore_id)
		REFERENCES chores (id)
		ON DELETE CASCADE
);

-- 9. ROTATION_ORDER
-- List<UUID> baseOrder з RotationSchedule — впорядкований список.
-- position: 0-based індекс в масиві.

CREATE TABLE rotation_order (
	chore_id UUID NOT NULL,
	position INT NOT NULL,
	user_id UUID NOT NULL,

	CONSTRAINT pk_rotation_order PRIMARY KEY (chore_id, position),
	CONSTRAINT uq_rotation_order_user UNIQUE (chore_id, user_id),
	CONSTRAINT fk_rotation_order_schedule FOREIGN KEY (chore_id)
		REFERENCES rotation_schedules (chore_id)
		ON DELETE CASCADE,
	CONSTRAINT fk_rotation_order_user FOREIGN KEY (user_id)
		REFERENCES users (id)
		ON DELETE CASCADE
);

-- 10. CHORE_COMPLETIONS
-- ChoreCompletion: факт виконання обов'язку за один цикл.
-- status: NOT_REQUIRED | PENDING | CONFIRMED | REJECTED
-- confirmed_by_user_id nullable: заповнюється тільки при CONFIRMED/REJECTED.

CREATE TABLE chore_completions (
	id UUID NOT NULL,
	chore_id UUID NOT NULL,
	completed_by_user_id UUID NOT NULL,
	completed_at TIMESTAMP WITH TIME ZONE NOT NULL,
	status VARCHAR(20) NOT NULL,
	confirmed_by_user_id UUID,
	confirmed_at TIMESTAMP WITH TIME ZONE,

	CONSTRAINT pk_chore_completions PRIMARY KEY (id),
	CONSTRAINT chk_chore_completions_status
		CHECK (status IN ('NOT_REQUIRED', 'PENDING', 'CONFIRMED', 'REJECTED')),
	-- confirmed_by і confirmed_at або обидва є (при CONFIRMED або REJECTED), або обидва null (при PENDING / NOT_REQUIRED).
	CONSTRAINT chk_chore_completions_confirmation
		CHECK (
		(confirmed_by_user_id IS NULL AND confirmed_at IS NULL) OR
		(confirmed_by_user_id IS NOT NULL AND confirmed_at IS NOT NULL)
		),
	CONSTRAINT fk_chore_completions_chore FOREIGN KEY (chore_id)
		REFERENCES chores (id)
		ON DELETE CASCADE,
	CONSTRAINT fk_chore_completions_executor FOREIGN KEY (completed_by_user_id)
		REFERENCES users (id)
		ON DELETE RESTRICT,
	CONSTRAINT fk_chore_completions_confirmer FOREIGN KEY (confirmed_by_user_id)
		REFERENCES users (id)
		ON DELETE RESTRICT
);

-- 11. SWAP_REQUESTS
-- SwapRequest: запит на обмін чергою між двома учасниками.
-- status: PENDING | ACCEPTED | REJECTED
-- swap_type: PERMANENT | TEMPORARY
-- cycle_number nullable: заповнюється тільки для TEMPORARY.

CREATE TABLE swap_requests (
	id UUID NOT NULL,
	chore_id UUID NOT NULL,
	initiator_user_id UUID NOT NULL,
	receiver_user_id UUID NOT NULL,
	status VARCHAR(20) NOT NULL,
	swap_type VARCHAR(20) NOT NULL,
	cycle_number INT,
	created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
	-- created_at: LocalDateTime у Java-коді (без timezone).
	-- При переході на PostgreSQL можна змінити на TIMESTAMP WITH TIME ZONE
	-- після того як Java-сторона буде оновлена до Instant.

	CONSTRAINT pk_swap_requests PRIMARY KEY (id),
	CONSTRAINT chk_swap_requests_status
		CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
	CONSTRAINT chk_swap_requests_swap_type
		CHECK (swap_type IN ('PERMANENT', 'TEMPORARY')),
	CONSTRAINT chk_swap_requests_initiator_receiver
		CHECK (initiator_user_id <> receiver_user_id),
	-- cycle_number обов'язковий для TEMPORARY, заборонений для PERMANENT.
	CONSTRAINT chk_swap_requests_cycle_number
		CHECK (
		(swap_type = 'TEMPORARY' AND cycle_number IS NOT NULL) OR
		(swap_type = 'PERMANENT' AND cycle_number IS NULL)
		),
	CONSTRAINT fk_swap_requests_chore FOREIGN KEY (chore_id)
		REFERENCES chores (id)
		ON DELETE CASCADE,
	CONSTRAINT fk_swap_requests_initiator FOREIGN KEY (initiator_user_id)
		REFERENCES users (id)
		ON DELETE RESTRICT,
	CONSTRAINT fk_swap_requests_receiver FOREIGN KEY (receiver_user_id)
		REFERENCES users (id)
		ON DELETE RESTRICT
);

-- 12. SCHEDULED_SWAPS
-- ScheduledSwap: відкладений обмін для TEMPORARY swap,
-- застосовується при advance() у відповідний цикл.

CREATE TABLE scheduled_swaps (
	id UUID NOT NULL,
	chore_id UUID NOT NULL,
	from_user_id UUID NOT NULL,
	to_user_id UUID NOT NULL,
	cycle_number INT NOT NULL,
	created_at TIMESTAMP WITH TIME ZONE NOT NULL,

	CONSTRAINT pk_scheduled_swaps PRIMARY KEY (id),
	CONSTRAINT fk_scheduled_swaps_chore FOREIGN KEY (chore_id)
		REFERENCES chores (id)
		ON DELETE CASCADE,
	-- RESTRICT, не CASCADE: якщо користувача видаляють, відкладений обмін
	-- має залишитись у БД і бути проігнорований в applyScheduledSwaps,
	-- а не зникнути тихо. Видалення користувача із активними scheduled_swaps
	-- потребує явної очистки на рівні сервісу.
	CONSTRAINT fk_scheduled_swaps_from FOREIGN KEY (from_user_id)
		REFERENCES users (id)
		ON DELETE RESTRICT,
	CONSTRAINT fk_scheduled_swaps_to FOREIGN KEY (to_user_id)
		REFERENCES users (id)
		ON DELETE RESTRICT
);

-- ІНДЕКСИ
-- Покривають найчастіші запити з коду.

-- chores: listChores фільтрує по household_id
CREATE INDEX idx_chores_household ON chores (household_id);

-- chore_completions: findByChoreId
CREATE INDEX idx_chore_completions_chore ON chore_completions (chore_id);

-- swap_requests: findByChoreId + existsPending (choreId, initiator, receiver, status)
CREATE INDEX idx_swap_requests_chore ON swap_requests (chore_id);
CREATE INDEX idx_swap_requests_pending
	ON swap_requests (chore_id, initiator_user_id, receiver_user_id, status);

-- scheduled_swaps: findByChoreIdAndCycleNumber + deleteExpired
CREATE INDEX idx_scheduled_swaps_chore_cycle
	ON scheduled_swaps (chore_id, cycle_number);

-- memberships: findByUserId
CREATE INDEX idx_memberships_user ON memberships (user_id);

-- invitation_codes: findByCode (вже покрито PK), findByHouseholdId (покрито UNIQUE)
-- Додаткових індексів не потрібно.

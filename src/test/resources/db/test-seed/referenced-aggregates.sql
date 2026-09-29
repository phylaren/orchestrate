INSERT INTO users (id, display_name, email, created_at) VALUES
    ('00000000-0000-0000-0000-0000000000a1', 'Seed Initiator', 'seed.initiator@example.com', CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-0000000000a2', 'Seed Receiver', 'seed.receiver@example.com', CURRENT_TIMESTAMP);

INSERT INTO households (id, name, owner_id, created_at) VALUES
    ('00000000-0000-0000-0000-0000000000b1', 'Seed Household', '00000000-0000-0000-0000-0000000000a1', CURRENT_TIMESTAMP);

INSERT INTO chores (id, household_id, name, description, recurrence_days, requires_confirmation, created_at) VALUES
    ('00000000-0000-0000-0000-0000000000c1', '00000000-0000-0000-0000-0000000000b1', 'Seed Chore', NULL, 7, TRUE, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-0000000000c2', '00000000-0000-0000-0000-0000000000b1', 'Seed Chore 2', NULL, 3, FALSE, CURRENT_TIMESTAMP);

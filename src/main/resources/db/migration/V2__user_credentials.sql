-- Orchestrate — V2__user_credentials.sql
--
-- Облікові дані для автентифікації (Spring Security).
--
-- password_hash: Argon2id-хеш у форматі PHC
--   NULL допустимий лише для облікових записів без пароля (створених поза реєстрацією,
--   напр. тестові дані): такий користувач не може увійти — UserDetails.isEnabled() = false.
-- enabled: прапорець блокування облікового запису; за замовчуванням активний.

ALTER TABLE users ADD COLUMN password_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN enabled BOOLEAN DEFAULT TRUE NOT NULL;
-- Orchestrate — V3__user_roles.sql
--
-- Роль користувача для вебінтерфейсу та адміністративних дій.
-- USER — звичайний користувач (значення за замовчуванням); ADMIN — адміністратор системи.

ALTER TABLE users ADD COLUMN role VARCHAR(20) DEFAULT 'USER' NOT NULL;
ALTER TABLE users ADD CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'));

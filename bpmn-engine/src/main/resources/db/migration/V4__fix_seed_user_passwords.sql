-- ============================================================
-- V4__fix_seed_user_passwords.sql
-- Updates the seed user password hashes to the correct BCrypt
-- hash for 'Alice@123' (the documented default test password).
-- The V2 seed data contained an incorrect pre-generated hash.
-- ============================================================

UPDATE engine_user
SET    password_hash = '$2a$10$KkrR7UlORn1D/4xnUcozAOhPMwne5n.KxhSIov0wSTGV6QqvGwcCa'
WHERE  id IN (
    'usr-alice', 'usr-bob',   'usr-carol', 'usr-dave',
    'usr-eve',   'usr-frank', 'usr-grace', 'usr-henry',
    'usr-ivy',   'usr-jack',  'usr-kate',  'usr-liam',
    'usr-mia'
);

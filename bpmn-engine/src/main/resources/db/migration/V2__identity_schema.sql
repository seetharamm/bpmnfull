-- ============================================================
-- V2__identity_schema.sql
-- Identity: users, roles, memberships (group membership)
-- ============================================================

-- -------------------------------------------------------
-- Role  (a named permission group, e.g. "admin", "hr_manager")
-- -------------------------------------------------------
CREATE TABLE role (
    id          VARCHAR(64)  NOT NULL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- -------------------------------------------------------
-- Group  (organisational group, e.g. "hr_team", "it_team")
-- -------------------------------------------------------
CREATE TABLE identity_group (
    id          VARCHAR(64)  NOT NULL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    type        VARCHAR(50)  NOT NULL DEFAULT 'ORGANISATIONAL',  -- ORGANISATIONAL | SECURITY | SYSTEM
    parent_id   VARCHAR(64),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_group_parent FOREIGN KEY (parent_id) REFERENCES identity_group(id)
);

-- -------------------------------------------------------
-- User
-- -------------------------------------------------------
CREATE TABLE engine_user (
    id            VARCHAR(64)  NOT NULL PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(100),
    last_name     VARCHAR(100),
    display_name  VARCHAR(200),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    locked        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

-- -------------------------------------------------------
-- User ↔ Role  (many-to-many)
-- -------------------------------------------------------
CREATE TABLE user_role (
    user_id    VARCHAR(64) NOT NULL,
    role_id    VARCHAR(64) NOT NULL,
    granted_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    granted_by VARCHAR(64),
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES engine_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES role(id)        ON DELETE CASCADE
);

-- -------------------------------------------------------
-- Membership  (User ↔ Group, many-to-many)
-- -------------------------------------------------------
CREATE TABLE membership (
    id         VARCHAR(64) NOT NULL PRIMARY KEY,
    user_id    VARCHAR(64) NOT NULL,
    group_id   VARCHAR(64) NOT NULL,
    joined_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    added_by   VARCHAR(64),
    CONSTRAINT uq_membership UNIQUE (user_id, group_id),
    CONSTRAINT fk_mem_user  FOREIGN KEY (user_id)  REFERENCES engine_user(id)     ON DELETE CASCADE,
    CONSTRAINT fk_mem_group FOREIGN KEY (group_id) REFERENCES identity_group(id)  ON DELETE CASCADE
);

-- -------------------------------------------------------
-- Group ↔ Role  (many-to-many — roles assigned to groups)
-- -------------------------------------------------------
CREATE TABLE group_role (
    group_id   VARCHAR(64) NOT NULL,
    role_id    VARCHAR(64) NOT NULL,
    granted_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id, role_id),
    CONSTRAINT fk_gr_group FOREIGN KEY (group_id) REFERENCES identity_group(id) ON DELETE CASCADE,
    CONSTRAINT fk_gr_role  FOREIGN KEY (role_id)  REFERENCES role(id)           ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_user_username ON engine_user(username);
CREATE INDEX idx_user_email    ON engine_user(email);
CREATE INDEX idx_mem_user      ON membership(user_id);
CREATE INDEX idx_mem_group     ON membership(group_id);
CREATE INDEX idx_ur_user       ON user_role(user_id);
CREATE INDEX idx_ur_role       ON user_role(role_id);

-- ============================================================
-- Seed data
-- ============================================================

-- ── Roles ────────────────────────────────────────────────────────────────────
INSERT INTO role (id, name, description) VALUES
  ('role-admin',       'ADMIN',           'Full administrative access to the engine'),
  ('role-process-mgr', 'PROCESS_MANAGER', 'Can deploy and manage process definitions'),
  ('role-operator',    'OPERATOR',        'Can start processes and manage running instances'),
  ('role-hr-manager',  'HR_MANAGER',      'Approves HR-related tasks'),
  ('role-hr-staff',    'HR_STAFF',        'Handles HR tasks'),
  ('role-it-staff',    'IT_STAFF',        'Handles IT setup and onboarding tasks'),
  ('role-finance',     'FINANCE',         'Handles finance-related tasks'),
  ('role-auditor',     'AUDITOR',         'Read-only access to history and reports'),
  ('role-developer',   'DEVELOPER',       'Can deploy processes and view engine internals');

-- ── Groups ───────────────────────────────────────────────────────────────────
INSERT INTO identity_group (id, name, description, type) VALUES
  ('grp-admin',    'administrators', 'System administrators',          'SECURITY'),
  ('grp-hr',       'hr_team',        'Human Resources department',     'ORGANISATIONAL'),
  ('grp-it',       'it_team',        'Information Technology team',    'ORGANISATIONAL'),
  ('grp-finance',  'finance_team',   'Finance & Accounting team',      'ORGANISATIONAL'),
  ('grp-ops',      'operations',     'Operations team',                'ORGANISATIONAL'),
  ('grp-audit',    'audit_team',     'Internal audit team',            'SECURITY'),
  ('grp-dev',      'developers',     'Software developers',            'ORGANISATIONAL');

-- ── Group → Role assignments ─────────────────────────────────────────────────
INSERT INTO group_role (group_id, role_id) VALUES
  ('grp-admin',   'role-admin'),
  ('grp-admin',   'role-process-mgr'),
  ('grp-admin',   'role-operator'),
  ('grp-hr',      'role-hr-manager'),
  ('grp-hr',      'role-hr-staff'),
  ('grp-it',      'role-it-staff'),
  ('grp-it',      'role-operator'),
  ('grp-finance', 'role-finance'),
  ('grp-ops',     'role-operator'),
  ('grp-audit',   'role-auditor'),
  ('grp-dev',     'role-developer'),
  ('grp-dev',     'role-process-mgr');

-- ── Users ────────────────────────────────────────────────────────────────────
-- Passwords are BCrypt hashes of the plaintext shown in comments.
-- All test passwords follow the pattern: FirstName@123  (e.g. alice → Alice@123)
-- Hash generated with BCrypt strength=10.

INSERT INTO engine_user (id, username, email, password_hash, first_name, last_name, display_name) VALUES
  -- Admins
  ('usr-alice',   'alice',   'alice@example.com',   '$2a$10$KkrR7UlORn1D/4xnUcozAOhPMwne5n.KxhSIov0wSTGV6QqvGwcCa', 'Alice',   'Admin',    'Alice Admin'),
  -- HR team
  ('usr-bob',     'bob',     'bob@example.com',     '$2a$10$KkrR7UlORn1D/4xnUcozAOhPMwne5n.KxhSIov0wSTGV6QqvGwcCa', 'Bob',     'Manager',  'Bob Manager'),
  ('usr-carol',   'carol',   'carol@example.com',   '$2a$10$KkrR7UlORn1D/4xnUcozAOhPMwne5n.KxhSIov0wSTGV6QqvGwcCa', 'Carol',   'HR',       'Carol HR'),
  ('usr-dave',    'dave',    'dave@example.com',    '$2a$10$KkrR7UlORn1D/4xnUcozAOhPMwne5n.KxhSIov0wSTGV6QqvGwcCa', 'Dave',    'HR',       'Dave HR'),
  -- IT team
  ('usr-eve',     'eve',     'eve@example.com',     '$2a$10$KkrR7UlORn1D/4xnUcozAOhPMwne5n.KxhSIov0wSTGV6QqvGwcCa', 'Eve',     'IT',       'Eve IT'),
  ('usr-frank',   'frank',   'frank@example.com',   '$2a$10$KkrR7UlORn1D/4xnUcozAOhPMwne5n.KxhSIov0wSTGV6QqvGwcCa', 'Frank',   'IT',       'Frank IT'),
  -- Finance team
  ('usr-grace',   'grace',   'grace@example.com',   '$2a$10$KkrR7UlORn1D/4xnUcozAOhPMwne5n.KxhSIov0wSTGV6QqvGwcCa', 'Grace',   'Finance',  'Grace Finance'),
  ('usr-henry',   'henry',   'henry@example.com',   '$2a$10$KkrR7UlORn1D/4xnUcozAOhPMwne5n.KxhSIov0wSTGV6QqvGwcCa', 'Henry',   'Finance',  'Henry Finance'),
  -- Operations
  ('usr-ivy',     'ivy',     'ivy@example.com',     '$2a$10$KkrR7UlORn1D/4xnUcozAOhPMwne5n.KxhSIov0wSTGV6QqvGwcCa', 'Ivy',     'Ops',      'Ivy Operations'),
  -- Audit
  ('usr-jack',    'jack',    'jack@example.com',    '$2a$10$KkrR7UlORn1D/4xnUcozAOhPMwne5n.KxhSIov0wSTGV6QqvGwcCa', 'Jack',    'Auditor',  'Jack Auditor'),
  -- Developers
  ('usr-kate',    'kate',    'kate@example.com',    '$2a$10$KkrR7UlORn1D/4xnUcozAOhPMwne5n.KxhSIov0wSTGV6QqvGwcCa', 'Kate',    'Dev',      'Kate Developer'),
  ('usr-liam',    'liam',    'liam@example.com',    '$2a$10$KkrR7UlORn1D/4xnUcozAOhPMwne5n.KxhSIov0wSTGV6QqvGwcCa', 'Liam',    'Dev',      'Liam Developer'),
  -- Cross-functional
  ('usr-mia',     'mia',     'mia@example.com',     '$2a$10$KkrR7UlORn1D/4xnUcozAOhPMwne5n.KxhSIov0wSTGV6QqvGwcCa', 'Mia',     'CrossFn',  'Mia Cross-Functional');

-- ── Direct user → role grants (in addition to group-inherited roles) ──────────
INSERT INTO user_role (user_id, role_id) VALUES
  ('usr-alice', 'role-admin'),
  ('usr-alice', 'role-process-mgr'),
  ('usr-bob',   'role-hr-manager'),
  ('usr-jack',  'role-auditor');

-- ── Memberships (users → groups) ─────────────────────────────────────────────
INSERT INTO membership (id, user_id, group_id) VALUES
  -- Admins group
  ('mem-001', 'usr-alice',  'grp-admin'),
  -- HR team
  ('mem-002', 'usr-bob',    'grp-hr'),
  ('mem-003', 'usr-carol',  'grp-hr'),
  ('mem-004', 'usr-dave',   'grp-hr'),
  -- IT team
  ('mem-005', 'usr-eve',    'grp-it'),
  ('mem-006', 'usr-frank',  'grp-it'),
  -- Finance team
  ('mem-007', 'usr-grace',  'grp-finance'),
  ('mem-008', 'usr-henry',  'grp-finance'),
  -- Operations
  ('mem-009', 'usr-ivy',    'grp-ops'),
  ('mem-010', 'usr-mia',    'grp-ops'),
  -- Audit
  ('mem-011', 'usr-jack',   'grp-audit'),
  -- Developers
  ('mem-012', 'usr-kate',   'grp-dev'),
  ('mem-013', 'usr-liam',   'grp-dev'),
  -- Cross-functional memberships
  ('mem-014', 'usr-mia',    'grp-hr'),
  ('mem-015', 'usr-kate',   'grp-ops');

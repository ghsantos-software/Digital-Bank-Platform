CREATE TABLE users (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    full_name   VARCHAR(150) NOT NULL,
    email       VARCHAR(150) NOT NULL,
    cpf         VARCHAR(14)  NOT NULL,
    birth_date  DATE         NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT pk_users         PRIMARY KEY (id),
    CONSTRAINT uk_users_email   UNIQUE (email),
    CONSTRAINT uk_users_cpf     UNIQUE (cpf),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED'))
);

CREATE INDEX idx_users_email  ON users (email);
CREATE INDEX idx_users_status ON users (status);

COMMENT ON TABLE  users             IS 'Platform users';
COMMENT ON COLUMN users.cpf         IS 'Brazilian individual taxpayer registry (CPF) — formatted as 000.000.000-00';
COMMENT ON COLUMN users.status      IS 'ACTIVE | INACTIVE (soft-deleted) | BLOCKED';

-- Sequence used by AccountNumberGenerator to produce unique account numbers
CREATE SEQUENCE account_number_seq
    START WITH 10000001
    INCREMENT BY 1
    NO CYCLE;

CREATE TABLE accounts (
    id             UUID           NOT NULL DEFAULT gen_random_uuid(),
    user_id        UUID           NOT NULL,
    account_number VARCHAR(20)    NOT NULL,
    branch         VARCHAR(10)    NOT NULL DEFAULT '0001',
    type           VARCHAR(20)    NOT NULL,
    balance        NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    status         VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP      NOT NULL DEFAULT now(),

    CONSTRAINT pk_accounts        PRIMARY KEY (id),
    CONSTRAINT uk_accounts_number UNIQUE (account_number),
    CONSTRAINT chk_accounts_type   CHECK (type   IN ('CHECKING', 'SAVINGS')),
    CONSTRAINT chk_accounts_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED')),
    CONSTRAINT chk_accounts_balance CHECK (balance >= 0)
);

CREATE INDEX idx_accounts_user_id ON accounts (user_id);
CREATE INDEX idx_accounts_status  ON accounts (status);


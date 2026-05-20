CREATE TABLE transactions (
    id                  UUID           NOT NULL DEFAULT gen_random_uuid(),
    idempotency_key     UUID           NOT NULL,
    source_account_id   UUID           NOT NULL,
    destination_account_id UUID,
    amount              NUMERIC(19, 2) NOT NULL,
    type                VARCHAR(20)    NOT NULL,
    status              VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    description         VARCHAR(255),
    failure_reason      VARCHAR(500),
    created_at          TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP      NOT NULL DEFAULT now(),

    CONSTRAINT pk_transactions               PRIMARY KEY (id),
    CONSTRAINT uk_transactions_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT chk_transactions_type   CHECK (type   IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER')),
    CONSTRAINT chk_transactions_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REVERSED')),
    CONSTRAINT chk_transactions_amount CHECK (amount > 0)
);

CREATE INDEX idx_transactions_source_account  ON transactions (source_account_id);
CREATE INDEX idx_transactions_dest_account    ON transactions (destination_account_id);
CREATE INDEX idx_transactions_status          ON transactions (status);
CREATE INDEX idx_transactions_created_at      ON transactions (created_at DESC);

COMMENT ON COLUMN transactions.idempotency_key      IS 'Client-supplied or auto-generated UUID — prevents duplicate processing';
COMMENT ON COLUMN transactions.source_account_id    IS 'For DEPOSIT: account credited. For WITHDRAWAL/TRANSFER: account debited.';
COMMENT ON COLUMN transactions.destination_account_id IS 'Only populated for TRANSFER — the account receiving the funds';
COMMENT ON COLUMN transactions.status               IS 'PENDING → COMPLETED | FAILED (set asynchronously via Kafka)';

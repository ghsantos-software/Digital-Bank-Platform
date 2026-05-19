ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS performed_by VARCHAR(255);

COMMENT ON COLUMN transactions.performed_by IS 'Keycloak user ID (JWT sub) who initiated the transaction';

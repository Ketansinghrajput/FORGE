-- V3__wallet_transactions.sql
-- Wallet transactions audit table for FORGE platform

CREATE TABLE IF NOT EXISTS wallet_transactions (
    id              BIGSERIAL PRIMARY KEY,
    wallet_id       BIGINT NOT NULL REFERENCES wallets(id),
    type            VARCHAR(10) NOT NULL CHECK (type IN ('CREDIT', 'DEBIT')),
    reason          VARCHAR(20) NOT NULL CHECK (reason IN ('BID_PLACED', 'BID_REFUND', 'PURCHASE', 'TOP_UP', 'WITHDRAWAL')),
    amount          NUMERIC(15, 2) NOT NULL,
    balance_after   NUMERIC(15, 2) NOT NULL,
    reference_id    BIGINT,
    description     VARCHAR(255),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

CREATE INDEX idx_wallet_tx_wallet_id ON wallet_transactions(wallet_id);
CREATE INDEX idx_wallet_tx_type ON wallet_transactions(wallet_id, type);
CREATE INDEX idx_wallet_tx_created_at ON wallet_transactions(created_at DESC);
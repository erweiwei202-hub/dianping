CREATE TABLE wallet_account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    available_balance_fen BIGINT NOT NULL DEFAULT 0,
    frozen_balance_fen BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_wallet_account_user UNIQUE (user_id),
    CONSTRAINT ck_wallet_available_nonnegative CHECK (available_balance_fen >= 0),
    CONSTRAINT ck_wallet_frozen_nonnegative CHECK (frozen_balance_fen >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE wallet_ledger (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    entry_type VARCHAR(32) NOT NULL,
    business_id VARCHAR(100) NOT NULL,
    amount_fen BIGINT NOT NULL,
    balance_after_fen BIGINT NOT NULL,
    description VARCHAR(200) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_wallet_ledger_business UNIQUE (user_id, entry_type, business_id),
    CONSTRAINT ck_wallet_ledger_nonzero CHECK (amount_fen <> 0),
    CONSTRAINT ck_wallet_ledger_balance_nonnegative CHECK (balance_after_fen >= 0),
    INDEX idx_wallet_ledger_user_created (user_id, created_at DESC, id DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE wallet_payment_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount_fen BIGINT NOT NULL,
    status VARCHAR(24) NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_wallet_payment_order UNIQUE (order_id),
    CONSTRAINT uk_wallet_payment_idempotency UNIQUE (idempotency_key),
    CONSTRAINT ck_wallet_payment_amount_positive CHECK (amount_fen > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE recharge_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    amount_fen BIGINT NOT NULL,
    channel VARCHAR(32) NOT NULL,
    channel_transaction_id VARCHAR(100) NULL,
    status VARCHAR(24) NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_recharge_idempotency UNIQUE (idempotency_key),
    CONSTRAINT uk_recharge_channel_tx UNIQUE (channel, channel_transaction_id),
    CONSTRAINT ck_recharge_amount_positive CHECK (amount_fen > 0),
    INDEX idx_recharge_user_created (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

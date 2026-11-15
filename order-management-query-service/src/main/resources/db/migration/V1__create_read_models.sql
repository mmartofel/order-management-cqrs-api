-- Read models of the query service. Every row keeps `version`: the sequence number of the last event of its
-- aggregate that was applied, so a redelivered or older event is skipped.

CREATE TABLE order_view (
    id            UUID           NOT NULL,
    person_id     BIGINT         NOT NULL,
    address_id    BIGINT         NOT NULL,
    status        VARCHAR(20)    NOT NULL,
    total         NUMERIC(14, 2) NOT NULL,
    placed_at     TIMESTAMPTZ    NOT NULL,
    payment_id    UUID,
    delivery_id   UUID,
    cancel_reason VARCHAR(500),
    updated_at    TIMESTAMPTZ    NOT NULL,
    version       BIGINT         NOT NULL,
    CONSTRAINT pk_order_view PRIMARY KEY (id),
    CONSTRAINT ck_order_view_status CHECK (status IN ('PLACED', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED'))
);

CREATE INDEX ix_order_view_person_id ON order_view (person_id);
CREATE INDEX ix_order_view_status ON order_view (status);

CREATE TABLE order_line_view (
    order_id   UUID           NOT NULL,
    line_no    INTEGER        NOT NULL,
    item_id    BIGINT         NOT NULL,
    sku        VARCHAR(50)    NOT NULL,
    name       VARCHAR(200)   NOT NULL,
    quantity   INTEGER        NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    CONSTRAINT pk_order_line_view PRIMARY KEY (order_id, line_no),
    CONSTRAINT fk_order_line_view_order FOREIGN KEY (order_id) REFERENCES order_view (id) ON DELETE CASCADE
);

CREATE TABLE payment_view (
    id                    UUID           NOT NULL,
    order_id              UUID           NOT NULL,
    amount                NUMERIC(14, 2) NOT NULL,
    method                VARCHAR(30)    NOT NULL,
    status                VARCHAR(20)    NOT NULL,
    transaction_reference VARCHAR(100),
    failure_reason        VARCHAR(500),
    refund_reason         VARCHAR(500),
    updated_at            TIMESTAMPTZ    NOT NULL,
    version               BIGINT         NOT NULL,
    CONSTRAINT pk_payment_view PRIMARY KEY (id),
    CONSTRAINT ck_payment_view_status CHECK (status IN ('INITIATED', 'COMPLETED', 'FAILED', 'REFUNDED'))
);

CREATE INDEX ix_payment_view_order_id ON payment_view (order_id);

CREATE TABLE delivery_view (
    id              UUID         NOT NULL,
    order_id        UUID         NOT NULL,
    address_id      BIGINT       NOT NULL,
    carrier         VARCHAR(100) NOT NULL,
    scheduled_for   DATE         NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    tracking_number VARCHAR(100),
    cancel_reason   VARCHAR(500),
    updated_at      TIMESTAMPTZ  NOT NULL,
    version         BIGINT       NOT NULL,
    CONSTRAINT pk_delivery_view PRIMARY KEY (id),
    CONSTRAINT ck_delivery_view_status CHECK (status IN ('SCHEDULED', 'SHIPPED', 'DELIVERED', 'CANCELLED'))
);

CREATE INDEX ix_delivery_view_order_id ON delivery_view (order_id);

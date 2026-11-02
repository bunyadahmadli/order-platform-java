CREATE TABLE orders (
    id          UUID         NOT NULL PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL,
    status      VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    version     BIGINT       NOT NULL,
    CONSTRAINT orders_status_check CHECK (status IN ('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'))
);

CREATE INDEX ix_orders_customer_created ON orders (customer_id, created_at DESC);

CREATE TABLE order_lines (
    order_id     UUID           NOT NULL REFERENCES orders (id),
    product_code VARCHAR(255)   NOT NULL,
    quantity     INTEGER        NOT NULL,
    unit_price   NUMERIC(19, 2) NOT NULL
);

CREATE INDEX ix_order_lines_order ON order_lines (order_id);

CREATE TABLE outbox_events (
    id           UUID         NOT NULL PRIMARY KEY,
    aggregate_id UUID         NOT NULL,
    event_type   VARCHAR(255) NOT NULL,
    routing_key  VARCHAR(255) NOT NULL,
    payload      TEXT         NOT NULL,
    created_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP(6) WITH TIME ZONE
);

CREATE INDEX ix_outbox_unpublished ON outbox_events (published_at, created_at);

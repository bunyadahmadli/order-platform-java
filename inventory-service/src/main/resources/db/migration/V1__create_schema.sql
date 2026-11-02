CREATE TABLE stock_items (
    product_code       VARCHAR(255) NOT NULL PRIMARY KEY,
    available_quantity INTEGER      NOT NULL,
    version            BIGINT       NOT NULL
);

CREATE TABLE processed_events (
    event_id     UUID NOT NULL PRIMARY KEY,
    processed_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

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

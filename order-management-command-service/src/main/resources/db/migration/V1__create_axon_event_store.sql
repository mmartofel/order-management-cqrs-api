-- Axon Framework 5.4.1 open-source storage: AggregateBasedJpaEventStorageEngine and JpaTokenStore.
-- Columns follow Axon's JPA entities (AggregateEventEntry, TokenEntry); Hibernate validates them at startup.
-- payload, metadata and token are @Lob byte[] in Axon, which Hibernate maps to PostgreSQL large objects (oid).

-- Global position of every event, used by streaming processors; allocation size 1 in Axon's mapping
CREATE SEQUENCE "aggregate-event-global-index-sequence" START WITH 1 INCREMENT BY 1;

-- The event store: append-only, one row per event. Never update or delete a row.
CREATE TABLE aggregate_event_entry (
    global_index              BIGINT       NOT NULL,
    aggregate_type            VARCHAR(255),
    aggregate_identifier      VARCHAR(255),
    aggregate_sequence_number BIGINT,
    type                      VARCHAR(255) NOT NULL,
    version                   VARCHAR(255) NOT NULL,
    timestamp                 VARCHAR(255) NOT NULL,
    payload                   OID          NOT NULL,
    metadata                  OID,
    identifier                VARCHAR(255) NOT NULL,
    CONSTRAINT pk_aggregate_event_entry PRIMARY KEY (global_index),
    -- the append conflict check: two commands appending the same sequence number of one aggregate
    CONSTRAINT uk_aggregate_event_entry_aggregate_seq UNIQUE (aggregate_identifier, aggregate_sequence_number),
    CONSTRAINT uk_aggregate_event_entry_identifier UNIQUE (identifier)
);

-- Positions of the event processors (the Kafka relay), one row per processor segment
CREATE TABLE token_entry (
    processor_name VARCHAR(255) NOT NULL,
    segment        INTEGER      NOT NULL,
    mask           INTEGER      NOT NULL,
    token          OID,
    token_type     VARCHAR(255),
    timestamp      VARCHAR(255) NOT NULL,
    owner          VARCHAR(255),
    CONSTRAINT pk_token_entry PRIMARY KEY (processor_name, segment)
);

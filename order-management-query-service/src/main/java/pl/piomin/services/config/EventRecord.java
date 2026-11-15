package pl.piomin.services.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * The headers the command service's relay puts on every event record. {@code sequence} is the aggregate's sequence
 * number of the event (0 for the first one); projections apply an event only when it is newer than the row.
 */
public record EventRecord(String type, String eventId, long sequence, Instant occurredAt, String payload) {

    public static final String EVENT_TYPE = "event-type";
    public static final String EVENT_ID = "event-id";
    public static final String SEQUENCE_NUMBER = "sequence-number";
    public static final String OCCURRED_AT = "occurred-at";

    /** Fails with {@link IllegalArgumentException} when a header is missing, which sends the record to the DLT. */
    public static EventRecord of(ConsumerRecord<String, String> record) {
        return new EventRecord(header(record, EVENT_TYPE), header(record, EVENT_ID),
                Long.parseLong(header(record, SEQUENCE_NUMBER)), Instant.parse(header(record, OCCURRED_AT)),
                record.value());
    }

    private static String header(ConsumerRecord<String, String> record, String name) {
        Header header = record.headers().lastHeader(name);
        if (header == null) {
            throw new IllegalArgumentException("Record at offset " + record.offset() + " has no " + name + " header");
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }
}

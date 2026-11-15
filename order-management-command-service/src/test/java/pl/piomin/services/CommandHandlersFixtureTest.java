package pl.piomin.services;

import org.axonframework.eventsourcing.configuration.EventSourcedEntityModule;
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer;
import org.axonframework.messaging.commandhandling.configuration.CommandHandlingModule;
import org.axonframework.modelling.repository.EntityNotFoundException;
import org.axonframework.test.fixture.AxonTestFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.piomin.services.config.AggregateTagResolver;
import pl.piomin.services.delivery.command.CancelDeliveryHandler;
import pl.piomin.services.delivery.command.CompleteDeliveryCommand;
import pl.piomin.services.delivery.command.CompleteDeliveryHandler;
import pl.piomin.services.delivery.command.ScheduleDeliveryCommand;
import pl.piomin.services.delivery.command.ScheduleDeliveryHandler;
import pl.piomin.services.delivery.command.ShipDeliveryCommand;
import pl.piomin.services.delivery.command.ShipDeliveryHandler;
import pl.piomin.services.delivery.domain.Delivery;
import pl.piomin.services.delivery.domain.event.DeliveryCompleted;
import pl.piomin.services.delivery.domain.event.DeliveryScheduled;
import pl.piomin.services.delivery.domain.event.DeliveryShipped;
import pl.piomin.services.exception.BusinessRuleViolationException;
import pl.piomin.services.exception.InvalidReferenceException;
import pl.piomin.services.order.command.CancelOrderCommand;
import pl.piomin.services.order.command.CancelOrderHandler;
import pl.piomin.services.order.command.PlaceOrderCommand;
import pl.piomin.services.order.command.PlaceOrderHandler;
import pl.piomin.services.order.domain.Order;
import pl.piomin.services.order.domain.event.OrderCancelled;
import pl.piomin.services.order.domain.event.OrderDelivered;
import pl.piomin.services.order.domain.event.OrderLine;
import pl.piomin.services.order.domain.event.OrderPaid;
import pl.piomin.services.order.domain.event.OrderPlaced;
import pl.piomin.services.order.domain.event.OrderShipped;
import pl.piomin.services.payment.command.CompletePaymentCommand;
import pl.piomin.services.payment.command.CompletePaymentHandler;
import pl.piomin.services.payment.command.FailPaymentHandler;
import pl.piomin.services.payment.command.InitiatePaymentCommand;
import pl.piomin.services.payment.command.InitiatePaymentHandler;
import pl.piomin.services.payment.command.RefundPaymentCommand;
import pl.piomin.services.payment.command.RefundPaymentHandler;
import pl.piomin.services.payment.domain.Payment;
import pl.piomin.services.payment.domain.event.PaymentCompleted;
import pl.piomin.services.payment.domain.event.PaymentInitiated;
import pl.piomin.services.payment.domain.event.PaymentRefunded;
import pl.piomin.services.reference.ReferenceClient.ItemRef;
import pl.piomin.services.reference.ReferenceData;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Given events, when a command, then the events the command appends - or the error and no events. */
class CommandHandlersFixtureTest {

    private static final Instant NOW = Instant.parse("2026-11-15T10:00:00Z");
    private static final BigDecimal TOTAL = new BigDecimal("39.98");

    private final ReferenceData referenceData = mock(ReferenceData.class);
    private final UUID orderId = UUID.randomUUID();
    private final UUID paymentId = UUID.randomUUID();
    private final UUID deliveryId = UUID.randomUUID();

    private AxonTestFixture fixture;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        EventSourcingConfigurer configurer = EventSourcingConfigurer.create()
                .registerTagResolver(c -> new AggregateTagResolver())
                .registerEntity(EventSourcedEntityModule.autodetected(UUID.class, Order.class))
                .registerEntity(EventSourcedEntityModule.autodetected(UUID.class, Payment.class))
                .registerEntity(EventSourcedEntityModule.autodetected(UUID.class, Delivery.class))
                .registerCommandHandlingModule(CommandHandlingModule.named("commands").commandHandlers()
                        .autodetectedCommandHandlingComponent(c -> new PlaceOrderHandler(referenceData, clock))
                        .autodetectedCommandHandlingComponent(c -> new CancelOrderHandler())
                        .autodetectedCommandHandlingComponent(c -> new InitiatePaymentHandler())
                        .autodetectedCommandHandlingComponent(c -> new CompletePaymentHandler())
                        .autodetectedCommandHandlingComponent(c -> new FailPaymentHandler())
                        .autodetectedCommandHandlingComponent(c -> new RefundPaymentHandler())
                        .autodetectedCommandHandlingComponent(c -> new ScheduleDeliveryHandler())
                        .autodetectedCommandHandlingComponent(c -> new ShipDeliveryHandler())
                        .autodetectedCommandHandlingComponent(c -> new CompleteDeliveryHandler())
                        .autodetectedCommandHandlingComponent(c -> new CancelDeliveryHandler()));
        fixture = AxonTestFixture.with(configurer);
    }

    @AfterEach
    void tearDown() {
        fixture.stop();
    }

    // --- Order ---

    @Test
    void placeOrder_validReferences_appendsOrderPlacedWithItemSnapshotAndTotal() {
        when(referenceData.requireItem(10L)).thenReturn(new ItemRef(10L, "SKU-10", "Mug", new BigDecimal("19.99")));

        fixture.given().noPriorActivity()
                .when().command(new PlaceOrderCommand(orderId, 1L, 2L,
                        List.of(new PlaceOrderCommand.Line(10L, 1), new PlaceOrderCommand.Line(10L, 1))))
                .then().success()
                .events(placed());
    }

    @Test
    void placeOrder_unknownItem_failsWithInvalidReferenceAndAppendsNothing() {
        when(referenceData.requireItem(anyLong())).thenThrow(new InvalidReferenceException("Item 99 does not exist"));

        fixture.given().noPriorActivity()
                .when().command(new PlaceOrderCommand(orderId, 1L, 2L, List.of(new PlaceOrderCommand.Line(99L, 1))))
                .then().exception(InvalidReferenceException.class)
                .noEvents();
    }

    @Test
    void placeOrder_addressOfAnotherPerson_failsAndAppendsNothing() {
        doThrow(new InvalidReferenceException("Address 2 does not belong to person 1"))
                .when(referenceData).requireAddressOf(eq(2L), eq(1L));

        fixture.given().noPriorActivity()
                .when().command(new PlaceOrderCommand(orderId, 1L, 2L, List.of(new PlaceOrderCommand.Line(10L, 1))))
                .then().exception(InvalidReferenceException.class)
                .noEvents();
    }

    @Test
    void cancelOrder_placed_appendsOrderCancelled() {
        fixture.given().events(placed())
                .when().command(new CancelOrderCommand(orderId, "changed my mind"))
                .then().success()
                .events(new OrderCancelled(orderId, "changed my mind"));
    }

    @Test
    void cancelOrder_shipped_failsWithRuleViolation() {
        fixture.given().events(placed(), new OrderPaid(orderId, paymentId), new OrderShipped(orderId, deliveryId))
                .when().command(new CancelOrderCommand(orderId, "too late"))
                .then().exception(BusinessRuleViolationException.class)
                .noEvents();
    }

    @Test
    void cancelOrder_unknownOrder_failsWithEntityNotFound() {
        fixture.given().noPriorActivity()
                .when().command(new CancelOrderCommand(UUID.randomUUID(), "no such order"))
                .then().exception(EntityNotFoundException.class)
                .noEvents();
    }

    // --- Payment ---

    @Test
    void initiatePayment_fullAmountForPlacedOrder_appendsPaymentInitiated() {
        fixture.given().events(placed())
                .when().command(new InitiatePaymentCommand(paymentId, orderId, TOTAL, "CARD"))
                .then().success()
                .events(new PaymentInitiated(paymentId, orderId, TOTAL, "CARD"));
    }

    @Test
    void initiatePayment_amountDiffersFromTotal_failsAndAppendsNothing() {
        fixture.given().events(placed())
                .when().command(new InitiatePaymentCommand(paymentId, orderId, new BigDecimal("1.00"), "CARD"))
                .then().exception(BusinessRuleViolationException.class)
                .noEvents();
    }

    @Test
    void initiatePayment_unknownOrder_failsWithInvalidReference() {
        fixture.given().noPriorActivity()
                .when().command(new InitiatePaymentCommand(paymentId, orderId, TOTAL, "CARD"))
                .then().exception(InvalidReferenceException.class)
                .noEvents();
    }

    @Test
    void completePayment_initiated_appendsPaymentCompletedAndOrderPaid() {
        fixture.given().events(placed(), new PaymentInitiated(paymentId, orderId, TOTAL, "CARD"))
                .when().command(new CompletePaymentCommand(paymentId, "TX-1"))
                .then().success()
                .events(new PaymentCompleted(paymentId, orderId, "TX-1"), new OrderPaid(orderId, paymentId));
    }

    @Test
    void completePayment_orderCancelledMeanwhile_failsAndAppendsNothing() {
        fixture.given().events(placed(), new PaymentInitiated(paymentId, orderId, TOTAL, "CARD"),
                        new OrderCancelled(orderId, "cancelled"))
                .when().command(new CompletePaymentCommand(paymentId, "TX-1"))
                .then().exception(BusinessRuleViolationException.class)
                .noEvents();
    }

    @Test
    void refundPayment_ofCancelledOrder_appendsPaymentRefunded() {
        fixture.given().events(placed(), new PaymentInitiated(paymentId, orderId, TOTAL, "CARD"),
                        new PaymentCompleted(paymentId, orderId, "TX-1"), new OrderPaid(orderId, paymentId),
                        new OrderCancelled(orderId, "cancelled"))
                .when().command(new RefundPaymentCommand(paymentId, "order cancelled"))
                .then().success()
                .events(new PaymentRefunded(paymentId, orderId, "order cancelled"));
    }

    @Test
    void refundPayment_notCompleted_failsWithRuleViolation() {
        fixture.given().events(placed(), new PaymentInitiated(paymentId, orderId, TOTAL, "CARD"),
                        new OrderCancelled(orderId, "cancelled"))
                .when().command(new RefundPaymentCommand(paymentId, "nothing to refund"))
                .then().exception(BusinessRuleViolationException.class)
                .noEvents();
    }

    // --- Delivery ---

    @Test
    void scheduleDelivery_paidOrder_appendsDeliveryScheduledToOrderAddress() {
        LocalDate date = LocalDate.of(2026, 11, 20);
        fixture.given().events(placed(), new OrderPaid(orderId, paymentId))
                .when().command(new ScheduleDeliveryCommand(deliveryId, orderId, "DHL", date))
                .then().success()
                .events(new DeliveryScheduled(deliveryId, orderId, 2L, "DHL", date));
    }

    @Test
    void scheduleDelivery_unpaidOrder_failsWithRuleViolation() {
        fixture.given().events(placed())
                .when().command(new ScheduleDeliveryCommand(deliveryId, orderId, "DHL", LocalDate.of(2026, 11, 20)))
                .then().exception(BusinessRuleViolationException.class)
                .noEvents();
    }

    @Test
    void shipDelivery_scheduled_appendsDeliveryShippedAndOrderShipped() {
        fixture.given().events(paidAndScheduled())
                .when().command(new ShipDeliveryCommand(deliveryId, "TRACK-1"))
                .then().success()
                .events(new DeliveryShipped(deliveryId, orderId, "TRACK-1"), new OrderShipped(orderId, deliveryId));
    }

    @Test
    void shipDelivery_twice_failsWithRuleViolation() {
        fixture.given().events(paidAndScheduled())
                .events(new DeliveryShipped(deliveryId, orderId, "TRACK-1"), new OrderShipped(orderId, deliveryId))
                .when().command(new ShipDeliveryCommand(deliveryId, "TRACK-2"))
                .then().exception(BusinessRuleViolationException.class)
                .noEvents();
    }

    @Test
    void completeDelivery_shipped_appendsDeliveryCompletedAndOrderDelivered() {
        fixture.given().events(paidAndScheduled())
                .events(new DeliveryShipped(deliveryId, orderId, "TRACK-1"), new OrderShipped(orderId, deliveryId))
                .when().command(new CompleteDeliveryCommand(deliveryId))
                .then().success()
                .events(new DeliveryCompleted(deliveryId, orderId), new OrderDelivered(orderId, deliveryId));
    }

    @Test
    void completeDelivery_notShipped_failsWithRuleViolation() {
        fixture.given().events(paidAndScheduled())
                .when().command(new CompleteDeliveryCommand(deliveryId))
                .then().exception(BusinessRuleViolationException.class)
                .noEvents();
    }

    private OrderPlaced placed() {
        return new OrderPlaced(orderId, 1L, 2L, List.of(new OrderLine(10L, "SKU-10", "Mug", 2, new BigDecimal("19.99"))),
                TOTAL, NOW);
    }

    private List<Object> paidAndScheduled() {
        return List.of(placed(), new OrderPaid(orderId, paymentId),
                new DeliveryScheduled(deliveryId, orderId, 2L, "DHL", LocalDate.of(2026, 11, 20)));
    }
}

package dev.lydtech.dispatch.integration;

import dev.lydtech.dispatch.DispatchConfiguration;
import dev.lydtech.dispatch.message.DispatchPreparing;
import dev.lydtech.dispatch.message.OrderCreated;
import dev.lydtech.dispatch.message.OrderDispatched;
import dev.lydtech.dispatch.util.TestEventData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpoint;
import org.springframework.kafka.config.KafkaListenerEndpointRegistrar;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

@Slf4j
@SpringBootTest(classes = {DispatchConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@EmbeddedKafka(controlledShutdown = true)
public class OrderDispatchIntegrationTest {

    private static final String ORDER_CREATED_TOPIC = "order.created";
    private static final String ORDER_DISPATCHED_TOPIC = "order.dispatched";
    private static final String DISPATCHED_TRACKING_TOPIC = "dispatched.tracking";

    @Autowired
    private KafkaTemplate kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Autowired
    private KafkaTestListener kafkaTestListener;

    @Configuration
    static class TestConfig {

        @Bean
        public KafkaTestListener testListener() {
            return new KafkaTestListener();
        }
    }

    public static class KafkaTestListener {
        AtomicInteger dispatchPreparingCounter = new AtomicInteger(0);
        AtomicInteger orderDispatchedCounter = new AtomicInteger(0);

        @KafkaListener(groupId = "dispatch.order.created.consumer", topics = DISPATCHED_TRACKING_TOPIC)
        void receiveDispatchPreparing(@Payload DispatchPreparing payload) {
            log.debug("Received DispatchedPreparing: {}", payload);
            dispatchPreparingCounter.incrementAndGet();
        }

        @KafkaListener(groupId = "dispatch.order.created.consumer", topics = ORDER_DISPATCHED_TOPIC)
        void receiveDispatchPreparing(@Payload OrderDispatched payload) {
            log.debug("Received OrderDispatched: {}", payload);
            orderDispatchedCounter.incrementAndGet();
        }
    }


    @BeforeEach
    public void setUp() {
        kafkaTestListener.dispatchPreparingCounter.set(0);
        kafkaTestListener.orderDispatchedCounter.set(0);

        registry.getListenerContainers().stream().forEach((container) -> {
            ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
        });
    }

    public void testOrderDispatchFlow() throws Exception {

        OrderCreated orderCreated = TestEventData.buildOrderCreatedEvent(UUID.randomUUID(), "new-item");
        sendMessage(ORDER_CREATED_TOPIC, orderCreated);

        await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(kafkaTestListener.dispatchPreparingCounter::get, equalTo(1));

        await().atMost(1, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(kafkaTestListener.orderDispatchedCounter::get, equalTo(1));
    }

    private void sendMessage(String topic, Object data) throws Exception {
        kafkaTemplate.send(MessageBuilder.withPayload(data)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build()).get();
    }
}

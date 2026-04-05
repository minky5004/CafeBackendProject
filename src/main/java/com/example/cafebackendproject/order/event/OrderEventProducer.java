package com.example.cafebackendproject.order.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class OrderEventProducer {

    private static final String TOPIC = "order.paid";
    private final KafkaTemplate<String, OrderEventPayload> kafkaTemplate;

    public void sendOrderPaidEvent(OrderEventPayload payload) {
        kafkaTemplate.send(TOPIC, String.valueOf(payload.getOrderId()), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka 이벤트 발행 실패 - orderId: {}", payload.getOrderId(), ex);
                    } else {
                        log.info("Kafka 이벤트 발행 성공 - orderId: {}, topic: {}", payload.getOrderId(), TOPIC);
                    }
                });
    }
}
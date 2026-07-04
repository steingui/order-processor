package com.example.orderprocessor.config;

import com.example.orderprocessor.service.OrderQueue;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    public MetricsConfig(OrderQueue orderQueue, MeterRegistry meterRegistry) {
        Gauge.builder("order.queue.size", orderQueue, OrderQueue::size)
                .description("Tamanho atual da fila de processamento de pedidos")
                .register(meterRegistry);
    }
}

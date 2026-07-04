package com.example.orderprocessor.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderWorkerPool {

    private final OrderQueue orderQueue;
    private final OrderService orderService;
    private final Tracer tracer;
    private final MeterRegistry meterRegistry;

    @Value("${app.workers.count:4}")
    private int workersCount;

    private ExecutorService executorService;

    @PostConstruct
    public void init() {
        log.info("Inicializando Worker Pool com {} threads", workersCount);
        executorService = Executors.newFixedThreadPool(workersCount);
        for (int i = 0; i < workersCount; i++) {
            executorService.submit(this::workerTask);
        }
    }

    private void workerTask() {
        log.info("Worker thread iniciada: {}", Thread.currentThread().getName());
        while (!Thread.currentThread().isInterrupted()) {
            OrderTask task = null;
            try {
                task = orderQueue.take();
                
                Span childSpan;
                if (task.parentSpan() != null) {
                    childSpan = tracer.nextSpan(task.parentSpan());
                } else {
                    childSpan = tracer.nextSpan();
                }
                childSpan.name("order-processing").start();

                try (Tracer.SpanInScope ws = tracer.withSpan(childSpan)) {
                    Timer.Sample sample = Timer.start(meterRegistry);
                    String statusTag = "completed";
                    try {
                        orderService.startProcessing(task.orderId());

                        log.info("Processando pedido ID={} na thread {}", task.orderId(), Thread.currentThread().getName());
                        Thread.sleep(3000); 

                        orderService.completeProcessing(task.orderId());
                    } catch (InterruptedException e) {
                        statusTag = "failed";
                        log.info("Worker thread interrompida durante o processamento do pedido: {}", task.orderId());
                        if (task.orderId() != null) {
                            orderService.failProcessing(task.orderId(), "Worker thread interrupted");
                        }
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        statusTag = "failed";
                        if (task.orderId() != null) {
                            orderService.failProcessing(task.orderId(), e.getMessage());
                        } else {
                            log.error("Erro inesperado no worker", e);
                        }
                    } finally {
                        sample.stop(meterRegistry.timer("order.processing.time", "status", statusTag));
                    }
                } finally {
                    childSpan.end();
                }
            } catch (InterruptedException e) {
                log.info("Worker thread interrompida no aguardo da fila: {}", Thread.currentThread().getName());
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Erro inesperado no loop principal do worker", e);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Finalizando Worker Pool...");
        if (executorService != null) {
            executorService.shutdownNow();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("Worker Pool não finalizou a tempo");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

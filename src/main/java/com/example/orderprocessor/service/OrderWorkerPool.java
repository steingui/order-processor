package com.example.orderprocessor.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderWorkerPool {

    private final OrderQueue orderQueue;
    private final OrderService orderService;

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
            UUID orderId = null;
            try {
                orderId = orderQueue.take();
                orderService.startProcessing(orderId);

                // Simula o tempo de processamento do pedido
                log.info("Processando pedido ID={} na thread {}", orderId, Thread.currentThread().getName());
                Thread.sleep(3000); 

                orderService.completeProcessing(orderId);
            } catch (InterruptedException e) {
                log.info("Worker thread interrompida: {}", Thread.currentThread().getName());
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                if (orderId != null) {
                    orderService.failProcessing(orderId, e.getMessage());
                } else {
                    log.error("Erro inesperado no worker", e);
                }
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

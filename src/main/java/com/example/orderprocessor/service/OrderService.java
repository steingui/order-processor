package com.example.orderprocessor.service;

import com.example.orderprocessor.dto.OrderRequest;
import com.example.orderprocessor.dto.OrderResponse;
import com.example.orderprocessor.model.Order;
import com.example.orderprocessor.model.OrderStatus;
import com.example.orderprocessor.repository.OrderRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderQueue orderQueue;
    private final Tracer tracer;
    private final MeterRegistry meterRegistry;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        Order order = Order.builder()
                .customerName(request.customerName())
                .totalAmount(request.totalAmount())
                .status(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);

        Span currentSpan = tracer.currentSpan();

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    orderQueue.submit(new OrderTask(savedOrder.getId(), currentSpan));
                    log.info("Pedido enfileirado após o commit da transação: ID={}", savedOrder.getId());
                }
            });
        } else {
            orderQueue.submit(new OrderTask(savedOrder.getId(), currentSpan));
            log.info("Pedido enfileirado sem transação ativa: ID={}", savedOrder.getId());
        }

        meterRegistry.counter("order.created", "status", "success").increment();
        log.info("Pedido criado no banco: ID={}, Cliente={}", savedOrder.getId(), savedOrder.getCustomerName());
        return OrderResponse.fromEntity(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado: " + id));
        return OrderResponse.fromEntity(order);
    }

    @Transactional
    public void startProcessing(UUID id) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            log.warn("Falha no worker: pedido não encontrado no banco: ID={}", id);
            return;
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Falha no worker: pedido ID={} com status inválido para processamento: {}", id, order.getStatus());
            return;
        }
        order.setStatus(OrderStatus.PROCESSING);
        log.info("Pedido marcado como PROCESSING: ID={}", id);
    }

    @Transactional
    public void completeProcessing(UUID id) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            log.warn("Falha no worker ao concluir: pedido não encontrado: ID={}", id);
            return;
        }
        if (order.getStatus() != OrderStatus.PROCESSING) {
            log.warn("Falha no worker ao concluir: pedido ID={} está com status inválido: {}", id, order.getStatus());
            return;
        }
        order.setStatus(OrderStatus.COMPLETED);
        meterRegistry.counter("order.processed", "status", "completed").increment();
        log.info("Pedido marcado como COMPLETED: ID={}", id);
    }

    @Transactional
    public void failProcessing(UUID id, String reason) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order != null) {
            order.setStatus(OrderStatus.FAILED);
            meterRegistry.counter("order.processed", "status", "failed").increment();
            log.error("Pedido marcado como FAILED: ID={}, Motivo={}", id, reason);
        } else {
            log.error("Falha no worker ao marcar como erro: pedido não encontrado: ID={}, Motivo={}", id, reason);
        }
    }
}

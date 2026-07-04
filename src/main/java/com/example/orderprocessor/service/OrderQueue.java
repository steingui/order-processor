package com.example.orderprocessor.service;

import org.springframework.stereotype.Component;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class OrderQueue {

    private final BlockingQueue<UUID> queue = new LinkedBlockingQueue<>();

    public void submit(UUID orderId) {
        queue.add(orderId);
    }

    public UUID take() throws InterruptedException {
        return queue.take();
    }

    public int size() {
        return queue.size();
    }
}

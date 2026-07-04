package com.example.orderprocessor.service;

import org.springframework.stereotype.Component;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class OrderQueue {

    private final BlockingQueue<OrderTask> queue = new LinkedBlockingQueue<>();

    public void submit(OrderTask task) {
        queue.add(task);
    }

    public OrderTask take() throws InterruptedException {
        return queue.take();
    }

    public int size() {
        return queue.size();
    }
}

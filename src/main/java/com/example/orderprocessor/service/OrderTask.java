package com.example.orderprocessor.service;

import io.micrometer.tracing.Span;
import java.util.UUID;

public record OrderTask(UUID orderId, Span parentSpan) {}

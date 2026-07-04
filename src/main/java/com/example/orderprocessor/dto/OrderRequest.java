package com.example.orderprocessor.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record OrderRequest(
    @NotBlank(message = "O nome do cliente é obrigatório")
    String customerName,

    @NotNull(message = "O valor total do pedido é obrigatório")
    @DecimalMin(value = "0.01", message = "O valor total deve ser no mínimo 0.01")
    BigDecimal totalAmount
) {}

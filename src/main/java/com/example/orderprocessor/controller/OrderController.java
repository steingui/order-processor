package com.example.orderprocessor.controller;

import com.example.orderprocessor.dto.OrderRequest;
import com.example.orderprocessor.dto.OrderResponse;
import com.example.orderprocessor.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Orders", description = "Endpoints para gerenciamento e processamento de pedidos")
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Criar um novo pedido", description = "Cria o pedido com status PENDING e o enfileira para processamento assíncrono.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Pedido aceito e enfileirado para processamento"),
            @ApiResponse(responseCode = "400", description = "Dados do pedido inválidos")
    })
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.accepted().body(response);
    }

    @Operation(summary = "Consultar status de um pedido", description = "Busca as informações detalhadas e o status atual do pedido pelo ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        OrderResponse response = orderService.getOrder(id);
        return ResponseEntity.ok(response);
    }
}

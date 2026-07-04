package com.example.orderprocessor;

import com.example.orderprocessor.dto.OrderRequest;
import com.example.orderprocessor.dto.OrderResponse;
import com.example.orderprocessor.model.OrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deveCriarEProcessarPedidoComSucesso() throws Exception {
        // 1. Criar Pedido
        OrderRequest request = new OrderRequest("Cliente Teste Integrado", new BigDecimal("150.75"));

        MvcResult createResult = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andReturn();

        String createResponseBody = createResult.getResponse().getContentAsString();
        OrderResponse createResponse = objectMapper.readValue(createResponseBody, OrderResponse.class);

        UUID orderId = createResponse.id();
        assertThat(orderId).isNotNull();
        assertThat(createResponse.customerName()).isEqualTo("Cliente Teste Integrado");
        assertThat(createResponse.status()).isEqualTo(OrderStatus.PENDING);

        // 2. Aguardar o processamento assíncrono pelo worker pool (worker demora 3 segundos)
        Thread.sleep(4500);

        // 3. Consultar status final do pedido
        MvcResult getResult = mockMvc.perform(get("/orders/" + orderId))
                .andExpect(status().isOk())
                .andReturn();

        String getResponseBody = getResult.getResponse().getContentAsString();
        OrderResponse getResponse = objectMapper.readValue(getResponseBody, OrderResponse.class);

        assertThat(getResponse.id()).isEqualTo(orderId);
        assertThat(getResponse.status()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void deveRetornarBadRequestQuandoDadosInvalidos() throws Exception {
        OrderRequest request = new OrderRequest("", new BigDecimal("-10.00"));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}

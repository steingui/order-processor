# Order Processor

Sistema de Processamento de Pedidos Assíncrono com Filas Locais em Spring Boot.

## Fluxo de Processamento de Pedidos

O diagrama abaixo ilustra como as requisições são recebidas, enfileiradas e processadas de forma assíncrona pelo sistema:

```mermaid
graph TD
    Cliente[Cliente] -->|POST /orders| API[REST API - OrderController]
    API -->|Salva PENDING & Enfileira| Fila[Fila de Trabalho / Worker Pool]
    Fila -->|Consome Pedido| Worker[Worker Thread]
    Worker -->|Atualiza Status (PROCESSING/COMPLETED/FAILED)| DB[(Banco de Dados)]
```

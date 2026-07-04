# Scripts de Teste de Carga

Este diretório contém os utilitários de simulação de carga concorrente para validação de observabilidade e performance do sistema.

## `load_test.py`

Script em Python puro (sem dependências externas) que simula o disparo de pedidos concorrentes para a API.

### Como Executar

A partir da raiz do projeto, execute:

```bash
python3 scripts/load_test.py
```

### Configurações

Você pode abrir o arquivo `load_test.py` e alterar as seguintes variáveis no topo do script para ajustar a intensidade do teste:

- `URL`: Endpoint de envio de pedidos (padrão: `http://localhost:8081/orders`).
- `TOTAL_REQUESTS`: Número total de requisições de criação de pedidos a serem enviadas (padrão: `30`).
- `CONCURRENCY`: Número máximo de threads simultâneas executando as requisições (padrão: `10`).

### Métricas Calculadas

O script exibe no console:
- Status das respostas (sucesso/falha).
- Latências de resposta calculadas estatisticamente: Mínima, Média, Máxima, **p95** e **p99**.
- Amostra de Correlation IDs (`X-Correlation-ID`) retornados nos cabeçalhos da resposta HTTP para auditoria e rastreamento.

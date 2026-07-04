import urllib.request
import urllib.parse
import json
import time
import random
import statistics
from concurrent.futures import ThreadPoolExecutor

URL = "http://localhost:8081/orders"
TOTAL_REQUESTS = 30
CONCURRENCY = 10

def send_order(client_id):
    payload = {
        "customerName": f"Cliente Carga #{client_id}",
        "totalAmount": round(random.uniform(10.0, 500.0), 2)
    }
    data = json.dumps(payload).encode('utf-8')
    req = urllib.request.Request(
        URL, 
        data=data, 
        headers={'Content-Type': 'application/json'}
    )
    start = time.time()
    try:
        with urllib.request.urlopen(req) as resp:
            latency = (time.time() - start) * 1000
            correlation_id = resp.headers.get("X-Correlation-ID", "N/A")
            status = resp.status
            body = json.loads(resp.read().decode('utf-8'))
            order_id = body.get("id")
            return {
                "success": status == 202,
                "latency": latency,
                "correlation_id": correlation_id,
                "order_id": order_id
            }
    except Exception as e:
        return {"success": False, "latency": 0, "error": str(e)}

def main():
    print(f"Iniciando teste de carga com {TOTAL_REQUESTS} pedidos (concorrência: {CONCURRENCY})...")
    
    with ThreadPoolExecutor(max_workers=CONCURRENCY) as executor:
        results = list(executor.map(send_order, range(TOTAL_REQUESTS)))
        
    latencies = [r["latency"] for r in results if r.get("success")]
    success_count = sum(1 for r in results if r.get("success"))
    
    print("\nResultados do Teste de Carga:")
    print(f"Total disparados: {TOTAL_REQUESTS}")
    print(f"Sucesso (202 Accepted): {success_count}")
    
    if latencies:
        print(f"Latência Mínima: {min(latencies):.2f} ms")
        print(f"Latência Média: {statistics.mean(latencies):.2f} ms")
        print(f"Latência p95: {statistics.quantiles(latencies, n=20)[18]:.2f} ms") # p95
        print(f"Latência p99: {statistics.quantiles(latencies, n=100)[98]:.2f} ms") # p99
        print(f"Latência Máxima: {max(latencies):.2f} ms")
        
        print("\nAmostra de Correlação de Tracing (Correlation IDs obtidos):")
        for r in results[:5]:
            if r.get("success"):
                print(f"  Pedido: {r['order_id']} | Correlation-ID: {r['correlation_id']}")

if __name__ == "__main__":
    main()

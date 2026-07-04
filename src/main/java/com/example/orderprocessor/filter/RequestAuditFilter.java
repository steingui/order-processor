package com.example.orderprocessor.filter;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE - 10) // Rodar tarde o suficiente para capturar o contexto do trace e processar a resposta corretamente
public class RequestAuditFilter extends OncePerRequestFilter {

    private final Tracer tracer;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Não auditar Swagger e Actuator endpoints para evitar logs desnecessários
        String uri = request.getRequestURI();
        if (uri.contains("/actuator") || uri.contains("/swagger-ui") || uri.contains("/v3/api-docs") || uri.contains("/h2-console")) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            String traceId = "N/A";
            if (tracer.currentSpan() != null && tracer.currentSpan().context() != null) {
                traceId = tracer.currentSpan().context().traceId();
            }
            
            // Define o Correlation ID no response header
            wrappedResponse.setHeader("X-Correlation-ID", traceId);

            String requestBody = getRequestBody(wrappedRequest);
            String responseBody = getResponseBody(wrappedResponse);

            log.info("Audit: Method={}, URI={}, Status={}, Duration={}ms, TraceID={}, RequestBody={}, ResponseBody={}",
                    wrappedRequest.getMethod(),
                    wrappedRequest.getRequestURI(),
                    wrappedResponse.getStatus(),
                    duration,
                    traceId,
                    requestBody,
                    responseBody);

            wrappedResponse.copyBodyToResponse();
        }
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] buf = request.getContentAsByteArray();
        if (buf.length > 0) {
            try {
                return new String(buf, 0, buf.length, request.getCharacterEncoding());
            } catch (Exception e) {
                return "[Error reading request body]";
            }
        }
        return "[Empty]";
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] buf = response.getContentAsByteArray();
        if (buf.length > 0) {
            try {
                return new String(buf, 0, buf.length, response.getCharacterEncoding());
            } catch (Exception e) {
                return "[Error reading response body]";
            }
        }
        return "[Empty]";
    }
}

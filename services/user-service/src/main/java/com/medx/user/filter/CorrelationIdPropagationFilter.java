package com.medx.user.filter;

import com.medx.user.context.CorrelationContext;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Servlet filter that extracts X-Correlation-Id from the incoming request header
 * (set by the API Gateway's CorrelationIdFilter) and stores it in CorrelationContext
 * for the duration of the request.
 * Runs at highest priority (Order 1) so the correlation ID is available to all subsequent
 * filters, interceptors, and service layers.
 * The finally block guarantees cleanup even if the request throws critical for Tomcat's
 * thread pool where threads are reused across requests.
 */
@Component
@Order(1)
@WebFilter("/*")
public class CorrelationIdPropagationFilter implements Filter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);

        try {
            CorrelationContext.set(correlationId != null ? correlationId : "");
            chain.doFilter(request, response);
        } finally {
            CorrelationContext.clear();
        }
    }
}
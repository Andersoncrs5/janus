package org.janus.shared.domain.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponseHTTP<T>(
        T data,
        String message,
        boolean success,
        String traceId,
        OffsetDateTime timestamp
) {

    public ResponseHTTP {
        if (timestamp == null) {
            timestamp = OffsetDateTime.now();
        }
        if (traceId == null) {
            traceId = resolveCurrentTraceId();
        }
    }

    public static <T> ResponseHTTP<T> ok(T data) {
        return new ResponseHTTP<>(data, "Success", true, null, null);
    }

    public static <T> ResponseHTTP<T> okTraceId(String id) {
        return new ResponseHTTP<>(null, "Success", true, id, null);
    }

    public static <T> ResponseHTTP<T> ok(T data, String traceId) {
        return new ResponseHTTP<>(data, null, true, traceId, null);
    }

    public static <T> ResponseHTTP<T> ok(T data, String traceId, String message) {
        return new ResponseHTTP<>(data, message, true, traceId, null);
    }

    public static <T> ResponseHTTP<T> error(String message) {
        return new ResponseHTTP<>(null, message, false, null, null);
    }

    public static <T> ResponseHTTP<T> error(String message, String customTraceId) {
        return new ResponseHTTP<>(null, message, false, customTraceId, null);
    }

    private static String resolveCurrentTraceId() {
        SpanContext spanContext = Span.current().getSpanContext();
        if (spanContext.isValid()) {
            return spanContext.getTraceId();
        }
        return null;
    }
}
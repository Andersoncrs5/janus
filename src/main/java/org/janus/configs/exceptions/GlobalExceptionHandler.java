package org.janus.configs.exceptions;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import org.janus.shared.domain.api.ResponseHTTP;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.util.HashMap;
import java.util.Map;

public class GlobalExceptionHandler {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionHandler.class);

    @ServerExceptionMapper
    public RestResponse<ResponseHTTP<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        LOG.warn("Parâmetro inválido recebido na requisição", ex);

        ResponseHTTP<Void> response = ResponseHTTP.error("Formato de parâmetro inválido: " + ex.getMessage());
        return RestResponse.status(Response.Status.BAD_REQUEST, response);
    }

    @ServerExceptionMapper
    public RestResponse<ResponseHTTP<Void>> handleBadRequest(BadRequestException ex) {
        LOG.warn("Requisição malformada capturada", ex);

        ResponseHTTP<Void> response = ResponseHTTP.error("Parâmetro ou corpo da requisição inválido.");
        return RestResponse.status(Response.Status.BAD_REQUEST, response);
    }

    @ServerExceptionMapper
    public RestResponse<ResponseHTTP<Void>> handleNullPointer(NullPointerException ex) {
        LOG.error("NullPointerException capturada pelo handler global", ex);

        ResponseHTTP<Void> response = ResponseHTTP.error("Referência nula encontrada no processamento da requisição.");
        return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR, response);
    }

    @ServerExceptionMapper
    public RestResponse<ResponseHTTP<Void>> handleInternalServerError(InternalServerErrorException ex) {
        Throwable cause = ex.getException() != null ? ex.getException() : ex;
        LOG.error("Erro interno capturado pelo handler global", cause);

        int statusCode = ex.getStatusCode() > 0 ? ex.getStatusCode() : Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        ResponseHTTP<Void> response = ResponseHTTP.error(ex.getMessage());

        return RestResponse.status(Response.Status.fromStatusCode(statusCode), response);
    }

    @ServerExceptionMapper
    public RestResponse<ResponseHTTP<Map<String, String>>> handleConstraint(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getConstraintViolations().forEach(v -> {
            String property = v.getPropertyPath().toString();
            String field = property.contains(".") ? property.substring(property.lastIndexOf('.') + 1) : property;
            errors.put(field, v.getMessage());
        });

        ResponseHTTP<Map<String, String>> response = new ResponseHTTP<>(
                errors,
                "Validation failed",
                false,
                null,
                null
        );

        return RestResponse.status(Response.Status.BAD_REQUEST, response);
    }

    @ServerExceptionMapper
    public RestResponse<ResponseHTTP<Void>> handleGenericException(Throwable ex) {
        LOG.error("Exceção não tratada capturada pelo handler global", ex);

        ResponseHTTP<Void> response = ResponseHTTP.error("Ocorreu um erro interno inesperado no servidor.");
        return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR, response);
    }
}
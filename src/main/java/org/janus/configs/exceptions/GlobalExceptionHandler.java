package org.janus.configs.exceptions;

import jakarta.validation.ConstraintViolationException;
import org.janus.shared.domain.api.ResponseHTTP;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.util.HashMap;
import java.util.Map;

public class GlobalExceptionHandler {

//    @ServerExceptionMapper
//    public RestResponse<ResponseHTTP<Map<String, String>>> handleConstraint(
//            ConstraintViolationException ex) {
//
//        Map<String, String> errors = new HashMap<>();
//
//        ex.getConstraintViolations().forEach(v -> {
//            String property = v.getPropertyPath().toString();
//            String field = property.substring(property.lastIndexOf('.') + 1);
//            errors.put(field, v.getMessage());
//        });
//
//        return RestResponse.status(Response.Status.BAD_REQUEST, new ResponseHTTP<>(
//                errors,
//                "Validation failed",
//                false
//        ));
//    }
//
//    @ServerExceptionMapper
//    public RestResponse<ResponseHTTP<Void>> handleNotAuthenticated(NotAuthenticatedException ex) {
//        var res = new ResponseHTTP<Void>(
//                null,
//                ex.getMessage(),
//                false
//        );
//
//        return RestResponse.status(Response.Status.UNAUTHORIZED, res);
//    }

}

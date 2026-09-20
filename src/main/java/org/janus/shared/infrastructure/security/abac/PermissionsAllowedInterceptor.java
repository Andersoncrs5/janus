package org.janus.shared.infrastructure.security.abac;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.json.JsonArray;
import jakarta.json.JsonString;
import jakarta.ws.rs.ForbiddenException;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.janus.shared.domain.result.Result;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@PermissionsAllowed
@Interceptor
@Priority(Interceptor.Priority.APPLICATION + 100)
public class PermissionsAllowedInterceptor {

    @Inject
    JsonWebToken jwt;

    @AroundInvoke
    public Object checkPermissions(InvocationContext context) throws Exception {

        PermissionsAllowed annotation =
                context.getMethod().getAnnotation(PermissionsAllowed.class);

        if (annotation == null) {
            annotation =
                    context.getTarget()
                            .getClass()
                            .getAnnotation(PermissionsAllowed.class);
        }

        String[] requiredPermissions = annotation.value();

        Set<String> userPermissions = extractPermissionsFromJwt();

        boolean hasPermission = Arrays.stream(requiredPermissions)
                .anyMatch(userPermissions::contains);

        if (!hasPermission) {

            String errorMessage =
                    "Acesso negado: permissão(ões) "
                            + Arrays.toString(requiredPermissions)
                            + " requerida(s).";

            if (Result.class.isAssignableFrom(
                    context.getMethod().getReturnType())) {

                return Result.forb(errorMessage);
            }

            throw new ForbiddenException(errorMessage);
        }

        return context.proceed();
    }

    private Set<String> extractPermissionsFromJwt() {

        if (jwt == null || jwt.getClaim("permissions") == null) {
            return Collections.emptySet();
        }

        Object claim = jwt.getClaim("permissions");

        if (claim instanceof JsonArray jsonArray) {
            return jsonArray.stream()
                    .map(val ->
                            val instanceof JsonString js
                                    ? js.getString()
                                    : val.toString()
                    )
                    .collect(Collectors.toSet());
        }

        if (claim instanceof Collection<?> collection) {
            return collection.stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet());
        }

        return Collections.emptySet();
    }
}
package org.janus.configs.mapperStruct.extensions;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonMapperUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static String toJson(Object object) {
        if (object == null) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (Exception e) {
            throw new IllegalArgumentException("Erro ao converter objeto para JSON", e);
        }
    }
}
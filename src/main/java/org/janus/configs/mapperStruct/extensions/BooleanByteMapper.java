package org.janus.configs.mapperStruct.extensions;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BooleanByteMapper {

    public boolean map(Byte value) {
        return value != null && value == 1;
    }

    public Byte map(boolean value) {
        return (byte) (value ? 1 : 0);
    }

    public boolean map(Integer value) {
        return value != null && value == 1;
    }

    public Integer mapInteger(boolean value) {
        return value ? 1 : 0;
    }
}
package org.janus.configs.mapperStruct.extensions;

public class EnumMapperUtils {

    public static String nameOrNull(Enum<?> enumValue) {
        return enumValue != null ? enumValue.name() : null;
    }

    public static <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(enumClass, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

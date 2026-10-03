package com.chuanqing.common.enums;

public enum IdentifierTypeEnums {
    PHONE,
    EMAIL;

    public static IdentifierTypeEnums fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("identifier type required");
        }
        return switch (value.toLowerCase()) {
            case "phone", "mobile" -> PHONE;
            case "email" -> EMAIL;
            default -> throw new IllegalArgumentException("Unsupported identifier type: " + value);
        };
    }
}

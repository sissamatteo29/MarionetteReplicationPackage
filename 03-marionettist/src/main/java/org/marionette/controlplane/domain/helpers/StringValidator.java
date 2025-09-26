package org.marionette.controlplane.domain.helpers;

import java.util.Objects;

public class StringValidator {

    public static String validateStringAndTrim(String input) {
        Objects.requireNonNull(input, "Found invalid null for string object");
        if(input.isBlank()) throw new IllegalArgumentException("The input string is blank");
        return input.trim();
    }
    
}

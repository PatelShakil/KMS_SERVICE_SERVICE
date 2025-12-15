package com.shakilpatel.service.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public class SlugGenerator {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern MULTIPLE_HYPHENS = Pattern.compile("-+");

    public static String generate(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutAccents = normalized.replaceAll("\\p{M}", "");
        String lowercase = withoutAccents.toLowerCase(Locale.ENGLISH);
        String withHyphens = WHITESPACE.matcher(lowercase).replaceAll("-");
        String cleaned = NON_LATIN.matcher(withHyphens).replaceAll("");
        String singleHyphens = MULTIPLE_HYPHENS.matcher(cleaned).replaceAll("-");

        return singleHyphens.replaceAll("^-|-$", "");
    }
}

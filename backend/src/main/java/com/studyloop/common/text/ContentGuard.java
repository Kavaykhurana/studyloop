package com.studyloop.common.text;

import com.studyloop.common.error.ApiException;
import com.studyloop.common.error.ErrorCode;
import java.util.Locale;
import java.util.regex.Pattern;

/** Server-side input hygiene: trims, collapses title whitespace, rejects script tags. */
public final class ContentGuard {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private ContentGuard() {}

    public static void rejectUnsafe(String... values) {
        for (String v : values) {
            if (v != null && v.toLowerCase(Locale.ROOT).contains("<script")) {
                throw new ApiException(ErrorCode.UNSAFE_CONTENT);
            }
        }
    }

    public static String title(String raw) {
        return raw == null ? null : WHITESPACE.matcher(raw.trim()).replaceAll(" ");
    }

    public static String text(String raw) {
        return raw == null ? null : raw.trim();
    }
}

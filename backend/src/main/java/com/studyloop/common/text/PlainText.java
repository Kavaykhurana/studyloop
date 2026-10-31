package com.studyloop.common.text;

import java.util.regex.Pattern;

/** Cheap Markdown-to-plain-text conversion for excerpts and prompt context. */
public final class PlainText {

    private static final Pattern FENCE = Pattern.compile("```[a-zA-Z0-9_+-]*");
    private static final Pattern IMAGE = Pattern.compile("!\\[([^\\]]*)\\]\\([^)]*\\)");
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]*)\\]\\([^)]*\\)");
    private static final Pattern HEADING = Pattern.compile("(?m)^\\s{0,3}#{1,6}\\s*");
    private static final Pattern BULLET = Pattern.compile("(?m)^\\s*(?:[-*+]|\\d+\\.)\\s+");
    private static final Pattern QUOTE = Pattern.compile("(?m)^\\s*>\\s?");
    private static final Pattern EMPHASIS = Pattern.compile("(\\*\\*|__|\\*|_|`|~~)");
    private static final Pattern SPACE = Pattern.compile("\\s+");

    private PlainText() {}

    public static String of(String markdown) {
        if (markdown == null) {
            return "";
        }
        String s = FENCE.matcher(markdown).replaceAll(" ");
        s = IMAGE.matcher(s).replaceAll("$1");
        s = LINK.matcher(s).replaceAll("$1");
        s = HEADING.matcher(s).replaceAll("");
        s = BULLET.matcher(s).replaceAll("");
        s = QUOTE.matcher(s).replaceAll("");
        s = EMPHASIS.matcher(s).replaceAll("");
        return SPACE.matcher(s).replaceAll(" ").trim();
    }

    /** Plain text cut to at most max characters, ending with an ellipsis when truncated. */
    public static String excerpt(String markdown, int max) {
        String plain = of(markdown);
        if (plain.length() <= max) {
            return plain;
        }
        return plain.substring(0, max).stripTrailing() + "…";
    }

    /** Plain text cut to at most max characters without an ellipsis (used for prompt context). */
    public static String clip(String markdown, int max) {
        String plain = of(markdown);
        return plain.length() <= max ? plain : plain.substring(0, max);
    }
}

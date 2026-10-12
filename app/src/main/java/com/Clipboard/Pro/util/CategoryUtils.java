package com.Clipboard.Pro.util;

import android.util.Patterns;

import java.util.regex.Pattern;

/** Fixed category list plus a light auto-detector. */
public final class CategoryUtils {

    public static final String GENERAL = "General";
    public static final String LINKS = "Links";
    public static final String EMAILS = "Emails";
    public static final String PHONES = "Phones";
    public static final String CODE = "Code";
    public static final String WORK = "Work";
    public static final String PERSONAL = "Personal";

    public static final String[] ALL = {GENERAL, LINKS, EMAILS, PHONES, CODE, WORK, PERSONAL};

    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9][0-9 ()\\-]{6,18}[0-9]$");
    private static final Pattern CODE_KEYWORD = Pattern.compile(
            "(?m)^\\s*(import\\s|from\\s+\\S+\\s+import\\s|def\\s|class\\s|function\\s|const\\s|let\\s|var\\s|public\\s|private\\s|#include|package\\s)");
    private static final Pattern HTML_TAG = Pattern.compile("</?[a-zA-Z][^>]*>");

    private CategoryUtils() {
    }

    public static String detect(String text) {
        if (text == null) {
            return GENERAL;
        }
        String t = text.trim();
        if (t.isEmpty()) {
            return GENERAL;
        }
        if (Patterns.EMAIL_ADDRESS.matcher(t).matches()) {
            return EMAILS;
        }
        if (isUrl(t)) {
            return LINKS;
        }
        if (PHONE.matcher(t).matches()) {
            return PHONES;
        }
        if (looksLikeCode(t)) {
            return CODE;
        }
        return GENERAL;
    }

    private static boolean isUrl(String t) {
        if (t.contains(" ") || t.contains("\n")) {
            return false;
        }
        String lower = t.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("www.");
    }

    private static boolean looksLikeCode(String t) {
        boolean symbols = t.contains(";") || t.contains("{") || t.contains("(") || t.contains("=");
        if (symbols && CODE_KEYWORD.matcher(t).find()) {
            return true;
        }
        if (t.contains("{") && t.contains("}") && t.contains(";")) {
            return true;
        }
        return t.contains("</") && HTML_TAG.matcher(t).find();
    }
}

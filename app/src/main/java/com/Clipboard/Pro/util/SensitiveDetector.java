package com.Clipboard.Pro.util;

import java.util.regex.Pattern;

/**
 * Best-effort heuristics that flag text which probably should not be stored (passwords, card
 * numbers, one-time codes, API keys...). It cannot be perfect, so the behaviour is user-controlled
 * in Settings.
 */
public final class SensitiveDetector {

    private static final Pattern CARD_SHAPE = Pattern.compile("^[0-9 \\-]{13,23}$");
    private static final Pattern OTP = Pattern.compile("^[0-9]{6}$");
    private static final Pattern OTP_PHRASE = Pattern.compile(
            "(?i)(\\botp\\b|verification code|one[- ]time (pass)?code|security code)");
    private static final Pattern SECRET_ASSIGN = Pattern.compile(
            "(?i)\\b(password|passwd|pwd|passcode|secret|api[_-]?key|access[_-]?token|auth[_-]?token|token)\\b\\s*[:=]\\s*\\S+");
    private static final Pattern BEARER = Pattern.compile("\\bBearer\\s+[A-Za-z0-9._\\-]{20,}");
    private static final Pattern JWT = Pattern.compile(
            "^eyJ[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+$");
    private static final Pattern KEY_PREFIX = Pattern.compile(
            "\\b(?:ghp_|gho_|github_pat_|sk-|AKIA|xox[baprs]-|AIza)[A-Za-z0-9_\\-]{16,}");
    private static final Pattern SSN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");

    private SensitiveDetector() {
    }

    public static boolean looksSensitive(String text) {
        if (text == null) {
            return false;
        }
        String t = text.trim();
        if (t.isEmpty()) {
            return false;
        }
        if (t.contains("-----BEGIN") && t.contains("PRIVATE KEY")) {
            return true;
        }
        if (OTP.matcher(t).matches()) {
            return true;
        }
        if (CARD_SHAPE.matcher(t).matches()) {
            String digits = t.replaceAll("[^0-9]", "");
            if (digits.length() >= 13 && digits.length() <= 19 && luhn(digits)) {
                return true;
            }
        }
        // Only scan the beginning of very long text; secrets are short.
        String head = t.length() > 4000 ? t.substring(0, 4000) : t;
        return OTP_PHRASE.matcher(head).find() && head.matches("(?s).*[0-9]{4,8}.*")
                || SECRET_ASSIGN.matcher(head).find()
                || BEARER.matcher(head).find()
                || JWT.matcher(t).matches()
                || KEY_PREFIX.matcher(head).find()
                || SSN.matcher(head).find();
    }

    private static boolean luhn(String digits) {
        int sum = 0;
        boolean alt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alt) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            alt = !alt;
        }
        return sum % 10 == 0;
    }
}

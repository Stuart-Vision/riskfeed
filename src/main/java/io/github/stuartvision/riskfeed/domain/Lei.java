package io.github.stuartvision.riskfeed.domain;

import java.util.regex.Pattern;

/**
 * Legal Entity Identifier (ISO 17442): 18 letters or digits followed by two
 * check digits, validated with ISO 7064 MOD 97-10 (the same scheme as IBAN).
 */
public final class Lei {

    private static final Pattern FORMAT = Pattern.compile("[A-Z0-9]{18}[0-9]{2}");

    private Lei() {
    }

    public static boolean isValid(String lei) {
        if (lei == null || !FORMAT.matcher(lei).matches()) {
            return false;
        }
        // Letters become two-digit numbers (A=10 ... Z=35); the whole number mod 97 must be 1.
        int remainder = 0;
        for (char c : lei.toCharArray()) {
            int value = Character.isDigit(c) ? c - '0' : c - 'A' + 10;
            remainder = (value > 9 ? remainder * 100 : remainder * 10) + value;
            remainder %= 97;
        }
        return remainder == 1;
    }
}

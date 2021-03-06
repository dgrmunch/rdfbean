package com.mysema.rdfbean.model.io;

import java.util.Locale;

/**
 * @author tiwe
 * 
 */
final class NTriplesUtil {

    public static String escapeString(String label) {
        int labelLength = label.length();
        StringBuilder sb = new StringBuilder(2 * labelLength);

        for (int i = 0; i < labelLength; i++) {
            char c = label.charAt(i);
            int cInt = c;

            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '"') {
                sb.append("\\\"");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (cInt >= 0x0 && cInt <= 0x8 || cInt == 0xB || cInt == 0xC
                    || cInt >= 0xE && cInt <= 0x1F || cInt >= 0x7F
                    && cInt <= 0xFFFF) {
                sb.append("\\u");
                sb.append(toHexString(cInt, 4));
            } else if (cInt >= 0x10000 && cInt <= 0x10FFFF) {
                sb.append("\\U");
                sb.append(toHexString(cInt, 8));
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    private static String toHexString(int decimal, int stringLength) {
        StringBuilder sb = new StringBuilder(stringLength);
        String hexVal = Integer.toHexString(decimal).toUpperCase(Locale.ENGLISH);
        // insert zeros if hexVal has less than stringLength characters:
        int nofZeros = stringLength - hexVal.length();
        for (int i = 0; i < nofZeros; i++) {
            sb.append('0');
        }
        sb.append(hexVal);
        return sb.toString();
    }

    private NTriplesUtil() {
    }

}

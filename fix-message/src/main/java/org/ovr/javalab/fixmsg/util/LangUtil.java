package org.ovr.javalab.fixmsg.util;

public abstract class LangUtil {
    public static int pow(long a, int b) {
        int result = 1;
        for (int i = 1; i <= b; i++) {
            result *= a;
        }
        return result;
    }
}

package org.geotools.mif.util;

public class MIFUtil {

    public static boolean startsWithIgnoreCase(String str, String with) {
        return str.regionMatches(true, 0, with, 0, with.length());
    }

}

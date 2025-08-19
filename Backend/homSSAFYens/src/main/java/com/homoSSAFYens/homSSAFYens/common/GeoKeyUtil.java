package com.homoSSAFYens.homSSAFYens.common;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class GeoKeyUtil {

    private GeoKeyUtil() {}

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd


    public static String dayKST() {
        return LocalDate.now(KST).format(DAY);
    }
    public static String dayKST(LocalDate date) {
        return date.format(DAY);
    }

    public static String geoKey(double lat, double lon, int decimals) {
        return round(lat, decimals) + "," + round(lon, decimals);
    }

    public static double round(double v, int d) {
        double m = Math.pow(10, d);
        return Math.round(v * m) / m;
    }
}

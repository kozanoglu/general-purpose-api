package com.n26challenge.util;

import java.util.Calendar;
import java.util.Date;

public class TimeUtil {

    private static final int MILLISECONDS = 60000;

    public static boolean isWithinLastMinute(Long timestamp) {
        return (System.currentTimeMillis() - timestamp) < MILLISECONDS;
    }

    public static int getSecondFromTimestamp(Long timestamp) {
        Date date = new Date(timestamp);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.SECOND);
    }
}

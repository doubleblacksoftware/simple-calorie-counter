package com.doubleblacksoftware.caloriecounter;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by x on 6/12/15.
 */
public class Utils {
    public static int dateToPagerIndex(Date date, int todayIndex) {
        Calendar cal = Calendar.getInstance();
        long dayDifference = (date.getTime() - cal.getTime().getTime())/(1000 * 60 * 60 * 24);
        return todayIndex + (int)dayDifference;
    }
}

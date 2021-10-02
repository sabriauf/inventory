package lk.sabri.inventory.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TimeFormatter {
    public final static long ONE_SECOND = 1000;
    private final static long SECONDS = 60;

    public final static long ONE_MINUTE = ONE_SECOND * SECONDS;
    private final static long MINUTES = 60;

    public final static long ONE_HOUR = ONE_MINUTE * MINUTES;
    private final static long HOURS = 24;

    public final static long ONE_DAY = ONE_HOUR * HOURS;
    private final static long DAYS_PER_MONTH = 30;

    public final static long ONE_MONTH = ONE_DAY * DAYS_PER_MONTH;
    private final static long MONTHS_PER_YEAR = 12;

    public final static long ONE_YEAR = ONE_MONTH * MONTHS_PER_YEAR;

    public static final String TIME_FORMAT = "HH:mm";
    public static final String DATE_FORMAT_LONG = "yyyy-MMMM-d";
    public static final String DATE_FORMAT_SHORT = "yyyy-MMM-d";
    public static final String DATE_FORMAT_DEFAULT = "yyyy-MM-dd";
    public static final String DETAIL_DATE_FORMAT = "EEE MMM dd HH:mm:ss zzz yyyy";
    public static final String DETAIL_TIMEZONE_DATE_FORMAT = "EEE MMM dd HH:mm:ss z yyyy";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static String getFormattedDate(String timeFormat, Date date, Locale locale) {
        SimpleDateFormat sdf = new SimpleDateFormat(timeFormat, locale);
        return sdf.format(date);
    }

    public static String getFormattedDate(String timeFormat, Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(timeFormat, Locale.getDefault());
        return sdf.format(date);
    }

    public static Date getFormattedDate(String timeFormat, String date, Locale locale) {
        SimpleDateFormat sdf = new SimpleDateFormat(timeFormat, locale);
        Date converted_date = null;
        try {
            converted_date = sdf.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return converted_date;
    }

    public static Date getFormattedDate(String timeFormat, String date) {
        return getFormattedDate(timeFormat, date, Locale.getDefault());
    }

    public static boolean isToday(Date date) {
        String formatedDate = getFormattedDate("yyyy-MM-dd", date);
        String formatedCurrentDate = getFormattedDate("yyyy-MM-dd", Calendar.getInstance().getTime());

        return formatedDate.equals(formatedCurrentDate);
    }

//    public static Long getDifferenceInDays(long duration, Locale locale) {
//        Date today = getFormattedDate(DATE_FORMAT_LONG, getFormattedDate(DATE_FORMAT_LONG, new Date(System.currentTimeMillis()), locale), locale);
//        Date inputDate = getFormattedDate(DATE_FORMAT_LONG, getFormattedDate(DATE_FORMAT_LONG, new Date(duration), locale), locale);
//
//        if (today != null && inputDate != null)
//            return ((today.getTime() - inputDate.getTime()) / (ONE_DAY));
//        else
//            return 0L;
//    }

//    private static StringBuilder buildFullRelativeString(StringBuilder res, Locale locale, Context context,
//                                                         long value, int string, boolean more) {
//        return res.append(new StringBuilder(String.format(locale, AppHandler.getString(context, string),
//                value)).append(more ? ", " : ""));
//    }
}

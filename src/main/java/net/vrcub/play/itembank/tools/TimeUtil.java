package net.vrcub.play.itembank.tools;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtil {
    public static boolean isToday(String dateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try {
            Date inputDate = sdf.parse(dateString);
            Date today = new Date();
            String todayString = sdf.format(today);

            Date formattedToday = sdf.parse(todayString);

            return inputDate.equals(formattedToday);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }
}

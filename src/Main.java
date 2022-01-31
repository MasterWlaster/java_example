import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class Main {
    public static void main(String[] args) {
        String source = JSONer.ReadFile("src/tickets.json");
        Object obj = JSONer.Parse(source);
        JSONObject json = (JSONObject) obj;

        JSONArray tickets = (JSONArray) json.get("tickets");

        Iterator itr = tickets.iterator();

        ArrayList<Long> listOfTimeDiffs = new ArrayList<Long>();

        while (itr.hasNext()) {
            JSONObject t = (JSONObject) itr.next();
            String origin = (String) t.get("origin_name");
            String destination = (String) t.get("destination_name");

            ZonedDateTime dt1 = null;
            ZonedDateTime dt2 = null;

            if (origin.equals("Владивосток") && destination.equals("Тель-Авив")) {
                dt1 = ZonedDateTime.parse(String.format(
                        DateTime.DATE_FORMAT,
                        DateTime.DateISO(t.get("departure_date")),
                        t.get("departure_time").toString().length() == 5
                                ? t.get("departure_time")
                                : "0" + t.get("departure_time"),
                        "+10:00[Asia/Vladivostok]"
                ));
                dt2 = ZonedDateTime.parse(String.format(
                        DateTime.DATE_FORMAT,
                        DateTime.DateISO(t.get("arrival_date")),
                        t.get("arrival_time").toString().length() == 5
                                ? t.get("arrival_time")
                                : "0" + t.get("arrival_time"),
                        "+03:00[Asia/Tel_Aviv]"
                ));
            }

            if (origin.equals("Тель-Авив") && destination.equals("Владивосток")) {
                dt1 = ZonedDateTime.parse(String.format(
                        DateTime.DATE_FORMAT,
                        DateTime.DateISO(t.get("arrival_date")),
                        t.get("arrival_time").toString().length() == 5
                                ? t.get("arrival_time")
                                : "0" + t.get("arrival_time"),
                        "+10:00[Asia/Vladivostok]"
                ));
                dt2 = ZonedDateTime.parse(String.format(
                        DateTime.DATE_FORMAT,
                        DateTime.DateISO(t.get("departure_date")),
                        t.get("departure_time").toString().length() == 5
                                ? t.get("departure_time")
                                : "0" + t.get("departure_time"),
                        "+03:00[Asia/Tel_Aviv]"
                ));
            }

            if (dt1 != null && dt2 != null) {
                listOfTimeDiffs.add(
                        DateTime.GetDifferenceInMinutes(dt1, dt2)
                );
            }
        }

        System.out.print("Average:\n" + DateTime.GetAverageTimeFrom(listOfTimeDiffs) + "\n\n");
        System.out.print("90 percentile:\n" + DateTime.GetPercentileTimeFrom(listOfTimeDiffs, 90) + "\n");
    }
}

/////////
class Percentile {
    public static Long Nearest(ArrayList<Long> arr, int percent) {
        int n = (int) Math.ceil(percent * arr.size() / 100f);
        Collections.sort(arr);
        return arr.get(n - 1);
    }

    public static double Interpolation(ArrayList<Long> arr, int percent, float c) {
        float n = (arr.size() + 1 - 2 * c) * percent / 100 + c;
        int x = (int) Math.floor(n);

        Collections.sort(arr);

        if (percent == 100) {
            return arr.get(arr.size() - 1);
        }

        double scale = Math.pow(10, 2);
        double xx = Math.ceil((n - x) * scale) / scale;

        return arr.get(x - 1) + xx * (arr.get(x) - arr.get(x - 1));
    }
}

/////////
class JSONer {
    public static String ReadFile(String fileName) {
        FileReader myFile = null;
        BufferedReader buff = null;
        try {
            myFile = new FileReader(fileName);
            buff = new BufferedReader(myFile);
            String json = "";
            while (true) {
                String newLine = buff.readLine();
                if (newLine == null) break;
                json += newLine;
            }
            return json;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                buff.close();
                myFile.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return null;
    }

    public static Object Parse(String input) {
        JSONParser parser = new JSONParser();
        Object obj = new Object();

        try {
            obj = parser.parse(input);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return obj;
    }
}

/////////
class DateTime {
    public static final String DATE_FORMAT = "%sT%s:00%s";

    public static long GetDifferenceInMinutes(ZonedDateTime dt1, ZonedDateTime dt2) {
        return ChronoUnit.MINUTES.between(dt1, dt2);
    }

    public static String DateISO(Object o) {
        String s = (String) o;
        String[] ss = s.split("\\.");

        return "20" + ss[2] + "-" + ss[1] + "-" + ss[0];
    }

    public static String GetAverageTimeFrom(ArrayList<Long> array) {
        long s = 0;
        for (long i : array) {
            s += i;
        }
        return formatMinutes(s / array.size());
    }

    public static String GetPercentileTimeFrom(ArrayList<Long> array, int percent) {
        Long m = Percentile.Nearest(array, percent);
        //Long m = (long) Percentile.Interpolation(array, percent, 0.5f);
        return formatMinutes(m);
    }

    static String formatMinutes(Long m) {
        return String.format("%d days  %d hours  %d minutes", m/60/24, m/60%24, m%60);
    }
}
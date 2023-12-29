import java.text.SimpleDateFormat;
import java.util.*;

public class ApacheLogGenerator {

    private static final Random random = new Random();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss -0800");

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java ApacheLogGenerator.java <totalNumLines> <timeSpan1> <timeSpan2> ...");
            System.out.println("Time Span Format: <startDate:endDate:weight>");
            System.out.println("Example to generate a year of logs over 1000 lines with heavier traffic in February and March:\n\t java ApacheLogGenerator.java 1000 " +
                    "2023-01-01:2023-02-01:1 " +
                    "2023-02-01:2023-03-01:3 " +
                    "2023-03-01:2023-04-01:5 " +
                    "2023-04-01:2023-05-01:1 " +
                    "2023-05-01:2023-06-01:1 " +
                    "2023-06-01:2023-07-01:1 " +
                    "2023-07-01:2023-08-01:1 " +
                    "2023-08-01:2023-09-01:1 " +
                    "2023-09-01:2023-10-01:1 " +
                    "2023-10-01:2023-11-01:1 " +
                    "2023-11-01:2023-12-01:1 ");

            System.exit(1);
        }

        int totalLines = Integer.parseInt(args[0]);
        List<TimeSpan> timeSpans = parseTimeSpans(Arrays.copyOfRange(args, 1, args.length));
        generateLogs(totalLines, timeSpans);
    }

    private static void generateLogs(int totalLines, List<TimeSpan> timeSpans) {
        int totalWeight = timeSpans.stream().mapToInt(span -> span.weight).sum();
        for (TimeSpan span : timeSpans) {
            long timeInterval = span.duration / (totalLines * span.weight / totalWeight);
            for (long t = 0; t < span.duration; t += timeInterval) {
                Date timestamp = new Date(span.start.getTime() + t);
                System.out.println(generateLogLine(timestamp));
            }
        }
    }

    private static String generateLogLine(Date timestamp) {
        return randomIp() + " - - [" + dateFormat.format(timestamp) + "] \"GET " + randomUrl() + " HTTP/1.1\" " +
                randomStatusCode() + " " + responseSize() + " \"-\" \"Mozilla/5.0\"";
    }


    private static List<TimeSpan> parseTimeSpans(String[] spans) {
        List<TimeSpan> timeSpans = new ArrayList<>();
        for (String span : spans) {
            String[] parts = span.split(":");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid time span format. Use <startDate:endDate:weight>");
            }
            Date start = parseDate(parts[0]);
            Date end = parseDate(parts[1]);
            int weight = Integer.parseInt(parts[2]);
            timeSpans.add(new TimeSpan(start, end, weight));
        }
        return timeSpans;
    }


    private static String randomIp() {
        return random.nextInt(256) + "." + random.nextInt(256) + "." +
                random.nextInt(256) + "." + random.nextInt(256);
    }

    private static String randomUrl() {
        String[] paths = {"action", "product", "item", "category", "blog"};
        return "http://" + randomIp() + "/" + paths[random.nextInt(paths.length)] + "/" + random.nextInt(9000) + 1000;
    }

    private static String randomStatusCode() {
        int[] codes = {200, 404, 500, 302};
        return Integer.toString(codes[random.nextInt(codes.length)]);
    }

    private static String responseSize() {
        return random.nextInt(9000) + 1000 + "";
    }

    private static Date parseDate(String dateString) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(dateString);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format. Use yyyy-MM-dd.");
        }
    }

    private static class TimeSpan {
        Date start;
        Date end;
        long duration;
        int weight;

        TimeSpan(Date start, Date end, int weight) {
            this.start = start;
            this.end = end;
            this.duration = end.getTime() - start.getTime();
            this.weight = weight;
        }
    }
}

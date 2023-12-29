import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class ApacheLogGenerator {

    private static final Random random = new Random();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss -0800");

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: ApacheLogGenerator <numLines> <startDate> <endDate>");
            System.exit(1);
        }

        int numLines = Integer.parseInt(args[0]);
        Date startDate = parseDate(args[1]);
        Date endDate = parseDate(args[2]);

        long duration = endDate.getTime() - startDate.getTime();
        long timeInterval = duration / numLines;

        for (int i = 0; i < numLines; i++) {
            Date timestamp = new Date(startDate.getTime() + i * timeInterval);
            System.out.println(generateLogLine(timestamp));
        }
    }

    private static String generateLogLine(Date timestamp) {
        return randomIp() + " - - [" + dateFormat.format(timestamp) + "] \"GET " + randomUrl() + " HTTP/1.1\" " +
                randomStatusCode() + " " + responseSize() + " \"-\" \"Mozilla/5.0\"";
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
}

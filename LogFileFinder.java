import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Scanner;

public class LogFileFinder {

    private static final int BUFFER_SIZE = 4096;
    private static final int PREVIEW_SIZE = 2000;
    private final Scanner scanner;
    private long fileSize;
    private long defaultLower;
    private long defaultUpper;
    private long lower;
    private long upper;
    private long mid;
    private File logFile;
    private String logFilePath;
    private String searchString;

    private enum SearchMode {
        AUTOMATIC,
        MANUAL
    }

    private SearchMode mode = SearchMode.MANUAL;

    public LogFileFinder() {
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) throws IOException {
        LogFileFinder finder = new LogFileFinder();
        finder.run(args);
    }

    public void run(String[] args) throws IOException {
        if (args.length < 1 || args.length > 5) {
            System.out.println("Usage: java LogFileFinder <path_to_log_file> [initial_lower_bound] [initial_upper_bound] [auto] [search_string]");
            System.out.println("If 'auto' is the fourth argument, the program will automatically iterate through the file to find the first line that contains the search string.");
            System.exit(1);
        }
        if (args.length == 5) {
            mode = SearchMode.AUTOMATIC;
            searchString = args[4];
        }

        logFilePath = args[0];
        logFile = new File(logFilePath);
        if (!logFile.isFile()) {
            System.out.println("File not found: " + logFilePath);
            return;
        }

        fileSize = logFile.length();
        defaultLower = args.length >= 2 ? Long.parseLong(args[1]) : 0;
        defaultUpper = args.length == 3 ? Long.parseLong(args[2]) : fileSize;

        if (mode == SearchMode.AUTOMATIC) {
            System.out.println("Searching for string: " + searchString);
            System.out.println("Lower bound: " + defaultLower);
            System.out.println("Upper bound: " + defaultUpper);
            lower = defaultLower;
            upper = defaultUpper;
        } else {
            lower = getUserInput("Enter the initial lower bound (in bytes)", defaultLower, 0, fileSize);
            upper = getUserInput("Enter the initial upper bound (in bytes)", defaultUpper, lower, fileSize);
        }

        previewBounds(logFile, lower, upper);

        while (lower < upper) {
            if (mode == SearchMode.AUTOMATIC) {
                if (iterateAutomatically()) {
                    printDDCommand();
                    break;
                }
            } else {
                if (iterateManually()) {
                    break;
                }
            }
        }
//        System.out.println("Script completed.");
    }

    private boolean iterateAutomatically() throws IOException {
        if (searchString == null) {
            System.out.println("Starting automatic search. Will iterate through file to find first line that contains search string.");
            System.out.println("Expecting the log file to have structure like this between lower and upper bounds.");
            System.out.println("In this case, the search string is \"BBBBBBB\".");
            System.out.println("AAAAAAA...");
            System.out.println("AAAAAAA...");
            System.out.println("AAAAAAA...");
            System.out.println("...");
            System.out.println("BBBBBBB...");
            System.out.println("BBBBBBB...");
            System.out.println("BBBBBBB...");

            System.out.println("Enter the search string:");
            searchString = scanner.nextLine().trim();
        }
        mid = (lower + upper) / 2;
        int bounds = (int) (upper - lower);
        int thresholdForRefinedSearch = BUFFER_SIZE * 10;
        String fileContents = readFileContentsAtOffset(logFile, mid, thresholdForRefinedSearch);

        String[] fileContentsLines = fileContents.split("\n");
        if (fileContentsLines.length < 2) {
            System.err.println("Expecting more lines within buffer size.");
            System.err.println("Mid: " + mid + ", Lower: " + lower + ", Upper: " + upper + ", Bounds: " + bounds);
            System.exit(1);
        }
        String firstLine = fileContentsLines[0]; //first line is likely partial
        String secondLine = fileContentsLines[1];

        if (bounds < thresholdForRefinedSearch && !secondLine.contains(searchString)) {
//            System.out.println("Bounds are small enough to iterate. Refining.");
            String[] lines = fileContents.split("\n");

            if (lines.length <= 2) {
                System.err.println("Expecting more lines within buffer size.");
                System.err.println("Mid: " + mid + ", Lower: " + lower + ", Upper: " + upper + ", Bounds: " + bounds);
                System.exit(1);
            }
            //assert the first line does not match the search string
            if (lines[0].contains(searchString)) {
                System.err.println("First line contains search string. This typically should not happen.");
                System.err.println("Mid: " + mid + ", Lower: " + lower + ", Upper: " + upper + ", Bounds: " + bounds);
                System.exit(1);
            }
            //assert the second line does not match the search string
            if (lines[1].contains(searchString)) {
                System.err.println("Second line contains search string. This might be okay, but be careful");
                System.err.println("Mid: " + mid + ", Lower: " + lower + ", Upper: " + upper + ", Bounds: " + bounds);
            }
            //assert the last line does match the search string
            if (!lines[lines.length - 1].contains(searchString)) {
                System.err.println("Last line does not contain search string. This typically should not happen.");
                System.err.println("Mid: " + mid + ", Lower: " + lower + ", Upper: " + upper + ", Bounds: " + bounds);
                System.exit(1);
            }

            //find the first line that matches the search string, counting all the characters along the way
            int offset = 0;
            for (String line : lines) {
                if (line.contains(searchString)) {
                    mid = mid + offset;
                    return true;
                }
                offset += line.length() + 1;
            }
        }

        if (secondLine.contains(searchString)) {
            //go lower
            upper = mid - 1;
        } else {
            //go higher
            lower = mid + 1;
        }
        return false;
    }

    private boolean iterateManually() throws IOException {
        mid = (lower + upper) / 2;
        previewContent(logFile, mid);
        long boundsSize = upper - lower;
        System.out.println("Current offset: " + mid + ", Bounds Size: " + boundsSize + ". Should I go (h)igher, (l)ower, (r)efine, (a)uto, or (q)uit? [h/l/r/q]");
        String response = scanner.nextLine().toLowerCase();

        if (response.startsWith("h")) {
            lower = mid + 1;
        } else if (response.startsWith("l")) {
            upper = mid - 1;
        } else if (response.startsWith("r")) {
            mid = promptUserForLineNumber(logFile, mid);

            printDDCommand();
            return true;
        } else if (response.startsWith("q")) {
            System.out.println("Quitting (lower: " + lower + ", upper: " + upper + ", mid: " + mid + ").");
            return true;
        } else if (response.startsWith("a")) {
            mode = SearchMode.AUTOMATIC;
        } else {
            System.out.println("Please answer higher, lower, or correct.");
        }
        return false;
    }

    private void printDDCommand() {
        System.out.println("Found offset: " + mid);
        System.out.println("Next time you run this script, you can use the following arguments to skip to this offset: \n\t" +
                logFilePath + " " + mid + " " + fileSize);

        String ddCommand = String.format("dd if=%s of=%s.clip iflag=skip_bytes,count_bytes,fullblock bs=4096 skip=%s count=%s",
                logFilePath, logFilePath, defaultLower, mid - defaultLower);
        System.out.println("Recommended dd command:\n\t" + ddCommand);
    }

    private long promptUserForLineNumber(File logFile, long mid) throws IOException {
        String fileContents = readFileContentsAtOffset(logFile, mid, BUFFER_SIZE);
        String[] lines = fileContents.split("\n");
        for(int i = 0; i < lines.length; i++) {
            System.out.println(i + ": " + lines[i]);
        }
        long lineNumber = getUserInput("Enter the line number of the line to split at", 1, 1, lines.length - 1);
        long distanceFromStart = 0;
        for(int i = 0; i < lineNumber; i++) {
            distanceFromStart += lines[i].length() + 1;
        }
        return mid + distanceFromStart;
    }

    private long getUserInput(String prompt, long defaultValue, long lower, long upper) {
        while (true) {
            System.out.println(prompt + " [" + lower + ", " + upper + "] (default " + defaultValue + "): ");
            String input = scanner.nextLine();
            if (input.isEmpty()) {
                return defaultValue;
            }
            try {
                long value = Long.parseLong(input);
                if (value >= lower && value <= upper) {
                    return value;
                } else {
                    System.out.println("Please enter a number between " + lower + " and " + upper + ".");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    private void previewBounds(File file, long lower, long upper) throws IOException {
        System.out.println("\n" + "=".repeat(40));
        System.out.println("Preview at Lower Bound:");
        printFileContents(file, lower, PREVIEW_SIZE);
        System.out.println("\n" + "=".repeat(40));

        System.out.println("Preview at Upper Bound:");
        printFileContents(file, upper, PREVIEW_SIZE);
        System.out.println("\n" + "=".repeat(40));
    }

    private void previewContent(File file, long offset) throws IOException {
        System.out.println();
        System.out.println("Preview at Current Offset:");
        System.out.println("=".repeat(40));
        printFileContents(file, offset, BUFFER_SIZE);
        System.out.println("\n" + "=".repeat(40));
    }

    private void printFileContents(File file, long offset, int bytesToRead) throws IOException {
        String buffer = readFileContentsAtOffset(file, offset, bytesToRead);
        int newlineIndex = findFirstNewline(buffer);
        if (newlineIndex != -1) {
            System.out.println("[possibly partial line (" + (newlineIndex + 1) + " chars)] " + buffer.substring(0, newlineIndex));
            String substring = buffer.substring(newlineIndex + 1);
            System.out.print(substring);
        } else {
            System.out.print(buffer);
        }
    }

    private String readFileContentsAtOffset(File file, long offset, int bytesToRead) throws IOException {
        long modifiedBytesToRead = Math.min(bytesToRead, (int) (upper - offset));
        if (modifiedBytesToRead > 0 && modifiedBytesToRead < bytesToRead) {
//            System.out.println("Reducing bytes to read from " + bytesToRead + " to " + modifiedBytesToRead);
            bytesToRead = (int) modifiedBytesToRead;
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(offset);
            byte[] buffer = new byte[bytesToRead];
            int bytesRead = raf.read(buffer);
            if (bytesRead != -1) {
                return new String(buffer, 0, bytesRead);
            }
        }
        return "";
    }

    private int findFirstNewline(String bufferString) {
        byte[] buffer = bufferString.getBytes();
        for (int i = 0; i < buffer.length; i++) {
            if (buffer[i] == '\n') {
                return i;
            }
        }
        return -1;
    }
}


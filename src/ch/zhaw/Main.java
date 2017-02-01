package ch.zhaw;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        String originalFile = "/home/slang/Documents/git/BookingAnalyzerGeocoding/test.csv";
        String newFile = "/home/slang/Documents/git/BookingAnalyzerGeocoding/test_new.csv";
        String currentWorkingLineFile = "/home/slang/Documents/git/BookingAnalyzerGeocoding/currentWorkingLine.txt";
        int currentWorkingLine = 0;
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(currentWorkingLineFile));
            String s = new String(bytes);
            currentWorkingLine = Integer.parseInt(s);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        String mapsKey = "AiF20pSFG81-Ja01AykfU_1Sd-L07ee0w6nxBGXYittfm7MkIPZfaWSBFsDxXKf7";
        String line = "";
        String cvsSplitBy = "@";

        FileInputStream inputStream = null;
        Scanner sc = null;

        try {
            inputStream = new FileInputStream(originalFile);
            sc = new Scanner(inputStream, "UTF-8");

            // Skip already processed files
            for (int i = 0; i < currentWorkingLine; i++) {
                sc.nextLine();
            }

            try (FileWriter fw = new FileWriter(newFile, true);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter out = new PrintWriter(bw)) {
                while (sc.hasNextLine()) {
                    line = sc.nextLine();

                    String[] splitLine = line.split(cvsSplitBy);

                    System.out.println("[a=" + splitLine[0] + ", b=" + splitLine[1] + ", b=" + splitLine[2] + "]");

                    String newLine;
                    if (currentWorkingLine == 0) {
                        newLine = line + "@longitude@latitude";
                    } else {
                        newLine = line + "@" + "dx" + "@ex";}

                    out.println(newLine);

                    currentWorkingLine++;
                    break;
                }
            }

            try (PrintWriter out = new PrintWriter(currentWorkingLineFile)) {
                out.print(currentWorkingLine);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (sc != null) {
                sc.close();
            }
        }
    }

}

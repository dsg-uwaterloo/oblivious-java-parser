package com.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;

public class SAMReadLengthCalculator {

    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++)
            oram.access("args[" + String.valueOf(i) + "]", Optional.ofNullable(args[i].getBytes()), true);
        if (args.length < 1) {
            System.out.println("Usage: java SAMReadLengthCalculator <sam_file>");
            return;
        }
        String samFile = new String(
                oram.access("args[" + String.valueOf(0) + "]", Optional.<byte[]>empty(), false).get());
        try {
            double averageReadLength = computeAverageReadLength(samFile);
            System.out.printf("Average Read Length: %.2f%n", averageReadLength);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static double computeAverageReadLength(String samFile) throws IOException {
        System.out.println(samFile);
        BufferedReader reader = new BufferedReader(new FileReader(samFile));
        String line;
        long totalReadLength = 0;
        long readCount = 0;
        while ((line = reader.readLine()) != null) {
            // Skip header lines
            if (line.startsWith("@")) {
                continue;
            }
            String[] fields = line.split("\t");
            for (int i = 0; i < fields.length; i++)
                oram.access("fields[" + String.valueOf(i) + "]", Optional.ofNullable(fields[i].getBytes()), true);
            if (fields.length > 9) {
                String readSequence = new String(
                        oram.access("fields[" + String.valueOf(9) + "]", Optional.<byte[]>empty(), false).get());
                totalReadLength += readSequence.length();
                readCount++;
            }
        }
        reader.close();
        if (readCount == 0) {
            throw new IOException("No reads found in the SAM file.");
        }
        return (double) totalReadLength / readCount;
    }

    private static PathORAM oram = new PathORAM(10000);
}
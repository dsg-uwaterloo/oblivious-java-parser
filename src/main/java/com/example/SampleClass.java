package com.example;

public class SampleClass {

    public static void main(String[] args) {
        if (args.length > 0) {
            System.out.println(args[0]);
            args[0] = "New value";
            System.out.println(args[0]);
        }
        String[] names = new String[2];
        names[0] = "Aaryan Shroff";
        names[1] = "John Doe";
        System.out.println(names[0]);
        System.out.println(names[1]);

        int[] date = new int[3];
        date[0] = 18;
        date[1] = 7;
        date[2] = 2024;
        System.out.println(date[0]);
        System.out.println(date[1]);
        System.out.println(date[2]);

        String[] sentence = new String[2];
        sentence[0] = "Hello";
        sentence[1] = "World";
        for (int i = 0; i < sentence.length; i++) {
            System.out.println(sentence[i]);
        }

        // If condition example
        if (date[0] >= 18) {
            date[0] = 1;
            date[1] = 5;
        } else if (date[0] >= 10) {
            date[0] = 2;
        }
        System.out.println("date[0]: " + date[0]);

        // For loop example
        int numVisits = 0;
        for (int i = 0; i < numVisits; i++) {
            System.out.println(i);
        }

        // 2D array example
        int r = 10;
        int c = 5;
        int[][] matrix = new int[r][c];

        int count = 0;
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                matrix[i][j] = count;
                count++;
            }
        }

        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                System.out.print(matrix[i][j] + " ");
            }
            System.out.print('\n');
        }

        // Function call example
        int sum = add(date[0], date[1]);
        System.out.println(sum);
    }

    private static int add(int a, int b) {
        return a + b;
    }
}

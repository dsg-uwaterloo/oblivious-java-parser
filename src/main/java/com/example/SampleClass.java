package com.example;

public class SampleClass {

    public static void main(String[] args) {
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
    }
}
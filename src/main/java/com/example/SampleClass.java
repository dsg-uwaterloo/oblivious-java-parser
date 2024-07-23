package com.example;

import java.util.Optional;
import java.nio.ByteBuffer;

public class SampleClass {

    public static void main(String[] args) {
        oram.access("names[" + String.valueOf(0) + "]", Optional.ofNullable("Aaryan Shroff".getBytes()), true);
        oram.access("names[" + String.valueOf(1) + "]", Optional.ofNullable("John Doe".getBytes()), true);
        System.out.println(
                new String(oram.access("names[" + String.valueOf(0) + "]", Optional.<byte[]>empty(), false).get()));
        System.out.println(
                new String(oram.access("names[" + String.valueOf(1) + "]", Optional.<byte[]>empty(), false).get()));
        oram.access("date[" + String.valueOf(0) + "]", Optional.ofNullable(ByteBuffer.allocate(4).putInt(18).array()),
                true);
        oram.access("date[" + String.valueOf(1) + "]", Optional.ofNullable(ByteBuffer.allocate(4).putInt(7).array()),
                true);
        oram.access("date[" + String.valueOf(2) + "]", Optional.ofNullable(ByteBuffer.allocate(4).putInt(2024).array()),
                true);
        System.out.println(ByteBuffer
                .wrap(oram.access("date[" + String.valueOf(0) + "]", Optional.<byte[]>empty(), false).get()).getInt());
        System.out.println(ByteBuffer
                .wrap(oram.access("date[" + String.valueOf(1) + "]", Optional.<byte[]>empty(), false).get()).getInt());
        System.out.println(ByteBuffer
                .wrap(oram.access("date[" + String.valueOf(2) + "]", Optional.<byte[]>empty(), false).get()).getInt());
        oram.access("sentence[" + String.valueOf(0) + "]", Optional.ofNullable("Hello".getBytes()), true);
        oram.access("sentence[" + String.valueOf(1) + "]", Optional.ofNullable("World".getBytes()), true);
        for (int i = 0; i < 2; i++) {
            System.out.println(new String(
                    oram.access("sentence[" + String.valueOf(i) + "]", Optional.<byte[]>empty(), false).get()));
        }
    }

    private static PathORAM oram = new PathORAM(10000);
}
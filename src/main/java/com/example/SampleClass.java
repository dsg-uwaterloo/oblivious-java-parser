package com.example;

import java.util.Optional;
import java.nio.ByteBuffer;

public class SampleClass {

  public static void main(String[] args) {
    String[] names;
    oram.access("names[0]", Optional.ofNullable("Aaryan Shroff".getBytes()), true);
    oram.access("names[1]", Optional.ofNullable("John Doe".getBytes()), true);
    System.out.println(new String(oram.access("names[0]", Optional.<byte[]>empty(), false).get()));
    System.out.println(new String(oram.access("names[1]", Optional.<byte[]>empty(), false).get()));
    int[] date;
    oram.access("date[0]", Optional.ofNullable(ByteBuffer.allocate(4).putInt(18).array()), true);
    oram.access("date[1]", Optional.ofNullable(ByteBuffer.allocate(4).putInt(7).array()), true);
    oram.access("date[2]", Optional.ofNullable(ByteBuffer.allocate(4).putInt(2024).array()), true);
    System.out.println(ByteBuffer.wrap(oram.access("date[0]", Optional.<byte[]>empty(), false).get()).getInt());
    System.out.println(ByteBuffer.wrap(oram.access("date[1]", Optional.<byte[]>empty(), false).get()).getInt());
    System.out.println(ByteBuffer.wrap(oram.access("date[2]", Optional.<byte[]>empty(), false).get()).getInt());
  }

  private static PathORAM oram = new PathORAM(10000);
}
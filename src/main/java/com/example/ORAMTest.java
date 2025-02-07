package com.example;

import java.nio.ByteBuffer;
import java.util.Optional;

public class ORAMTest {

    private static PathORAM oram = new PathORAM(1);

    public static void main(String[] args) {
        oram.access("A", Optional.ofNullable(ByteBuffer.allocate(4).putInt(1).array()), true);
        oram.access("B", Optional.ofNullable(ByteBuffer.allocate(4).putInt(2).array()), true);
        oram.access("C", Optional.ofNullable(ByteBuffer.allocate(4).putInt(3).array()), true);
        oram.access("D", Optional.ofNullable(ByteBuffer.allocate(4).putInt(4).array()), true);
        oram.access("E", Optional.ofNullable(ByteBuffer.allocate(4).putInt(5).array()), true);
        oram.access("F", Optional.ofNullable(ByteBuffer.allocate(4).putInt(6).array()), true);
        oram.access("G", Optional.ofNullable(ByteBuffer.allocate(4).putInt(7).array()), true);
        oram.access("H", Optional.ofNullable(ByteBuffer.allocate(4).putInt(8).array()), true);
        oram.access("I", Optional.ofNullable(ByteBuffer.allocate(4).putInt(9).array()), true);
        oram.access("J", Optional.ofNullable(ByteBuffer.allocate(4).putInt(10).array()), true);
        oram.access("K", Optional.ofNullable(ByteBuffer.allocate(4).putInt(11).array()), true);
        oram.access("L", Optional.ofNullable(ByteBuffer.allocate(4).putInt(12).array()), true);
        oram.access("M", Optional.ofNullable(ByteBuffer.allocate(4).putInt(13).array()), true);
        oram.access("N", Optional.ofNullable(ByteBuffer.allocate(4).putInt(14).array()), true);
        oram.access("O", Optional.ofNullable(ByteBuffer.allocate(4).putInt(15).array()), true);
        oram.access("P", Optional.ofNullable(ByteBuffer.allocate(4).putInt(16).array()), true);
        oram.access("Q", Optional.ofNullable(ByteBuffer.allocate(4).putInt(17).array()), true);
        oram.access("R", Optional.ofNullable(ByteBuffer.allocate(4).putInt(18).array()), true);
        oram.access("S", Optional.ofNullable(ByteBuffer.allocate(4).putInt(19).array()), true);
        oram.access("T", Optional.ofNullable(ByteBuffer.allocate(4).putInt(20).array()), true);
        oram.access("U", Optional.ofNullable(ByteBuffer.allocate(4).putInt(21).array()), true);
        oram.access("V", Optional.ofNullable(ByteBuffer.allocate(4).putInt(22).array()), true);
        oram.access("W", Optional.ofNullable(ByteBuffer.allocate(4).putInt(23).array()), true);
        oram.access("X", Optional.ofNullable(ByteBuffer.allocate(4).putInt(24).array()), true);
        oram.access("Y", Optional.ofNullable(ByteBuffer.allocate(4).putInt(25).array()), true);
        oram.access("Z", Optional.ofNullable(ByteBuffer.allocate(4).putInt(26).array()), true);

        System.out.println(oram.access("F", Optional.empty(), false).get());
        oram.prettyPrintTree();
        oram.printPositionMap();
    }
}

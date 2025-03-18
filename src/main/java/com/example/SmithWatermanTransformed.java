package com.example;

import java.nio.ByteBuffer;
import java.util.Optional;

public class SmithWatermanTransformed {

    public static void main(String[] args) {
        PathORAM oram = new PathORAM(256);
        for (int i = 0; i < args.length; i = i + 1) {
            oram.access("args[" + String.valueOf(i) + "]", Optional.ofNullable((args[i]).getBytes()), true);
        }
        if (args.length < 2) {
            System.out.println("Usage: java SmithWaterman <sequence1> <sequence2>");
            System.exit(1);
        } else {
        }
        oram.access("seq1", Optional.ofNullable(
                (new String(oram.access("args" + "[" + String.valueOf(0) + "]", Optional.<byte[]>empty(), false).get()))
                        .getBytes()),
                true);
        oram.access("seq2", Optional.ofNullable(
                (new String(oram.access("args" + "[" + String.valueOf(1) + "]", Optional.<byte[]>empty(), false).get()))
                        .getBytes()),
                true);
        oram.access("gapPenalty", Optional.ofNullable(ByteBuffer.allocate(4).putInt(-2).array()), true);
        oram.access("matchScore", Optional.ofNullable(ByteBuffer.allocate(4).putInt(2).array()), true);
        oram.access("mismatchScore", Optional.ofNullable(ByteBuffer.allocate(4).putInt(-1).array()), true);
        oram.access("rows", Optional.ofNullable(ByteBuffer.allocate(4)
                .putInt(new String(oram.access("seq1", Optional.<byte[]>empty(), false).get()).length() + 1).array()),
                true);
        oram.access("cols", Optional.ofNullable(ByteBuffer.allocate(4)
                .putInt(new String(oram.access("seq2", Optional.<byte[]>empty(), false).get()).length() + 1).array()),
                true);
        for (oram.access("i", Optional.ofNullable(ByteBuffer.allocate(4).putInt(0).array()), true); ByteBuffer
                .wrap(oram.access("i", Optional.<byte[]>empty(), false).get())
                .getInt() < _obliviousNextPowerOf2(ByteBuffer
                        .wrap(oram.access("rows", Optional.<byte[]>empty(), false).get()).getInt()); oram.access(
                                "i",
                                Optional.ofNullable(ByteBuffer.allocate(4).putInt(ByteBuffer
                                        .wrap(oram.access("i", Optional.<byte[]>empty(), false).get()).getInt() + 1)
                                        .array()),
                                true)) {
            if (ByteBuffer.wrap(oram.access("i", Optional.<byte[]>empty(), false).get()).getInt() < ByteBuffer
                    .wrap(oram.access("rows", Optional.<byte[]>empty(), false).get()).getInt()) {
                for (oram.access("j", Optional.ofNullable(ByteBuffer.allocate(4).putInt(0).array()), true); ByteBuffer
                        .wrap(oram.access("j", Optional.<byte[]>empty(), false).get())
                        .getInt() < _obliviousNextPowerOf2(ByteBuffer
                                .wrap(oram.access("cols", Optional.<byte[]>empty(), false).get()).getInt()); oram
                                        .access("j", Optional.ofNullable(ByteBuffer.allocate(4)
                                                .putInt(ByteBuffer
                                                        .wrap(oram.access("j", Optional.<byte[]>empty(), false).get())
                                                        .getInt() + 1)
                                                .array()), true)) {
                    if (ByteBuffer.wrap(oram.access("j", Optional.<byte[]>empty(), false).get()).getInt() < ByteBuffer
                            .wrap(oram.access("cols", Optional.<byte[]>empty(), false).get()).getInt()) {
                        oram.access(
                                "H" + "["
                                        + String.valueOf(ByteBuffer
                                                .wrap(oram.access("i", Optional.<byte[]>empty(), false).get()).getInt())
                                        + "]" + "["
                                        + String.valueOf(ByteBuffer
                                                .wrap(oram.access("j", Optional.<byte[]>empty(), false).get()).getInt())
                                        + "]",
                                Optional.ofNullable(ByteBuffer.allocate(4).putInt(0).array()), true);
                    } else {
                        oram.access("dummy", Optional.<byte[]>empty(), false);
                    }
                }
            } else {
                oram.access("dummy", Optional.<byte[]>empty(), false);
                oram.access("dummy", Optional.<byte[]>empty(), false);
            }
        }
        oram.access("maxScore", Optional.ofNullable(ByteBuffer.allocate(4).putInt(0).array()), true);
        oram.access("maxI", Optional.ofNullable(ByteBuffer.allocate(4).putInt(0).array()), true);
        oram.access("maxJ", Optional.ofNullable(ByteBuffer.allocate(4).putInt(0).array()), true);
        for (oram.access("i", Optional.ofNullable(ByteBuffer.allocate(4).putInt(1).array()), true); ByteBuffer
                .wrap(oram.access("i", Optional.<byte[]>empty(), false).get())
                .getInt() < _obliviousNextPowerOf2(ByteBuffer
                        .wrap(oram.access("rows", Optional.<byte[]>empty(), false).get()).getInt()); oram.access(
                                "i",
                                Optional.ofNullable(ByteBuffer.allocate(4).putInt(ByteBuffer
                                        .wrap(oram.access("i", Optional.<byte[]>empty(), false).get()).getInt() + 1)
                                        .array()),
                                true)) {
            if (ByteBuffer.wrap(oram.access("i", Optional.<byte[]>empty(), false).get()).getInt() < ByteBuffer
                    .wrap(oram.access("rows", Optional.<byte[]>empty(), false).get()).getInt()) {
                for (oram.access("j", Optional.ofNullable(ByteBuffer.allocate(4).putInt(1).array()), true); ByteBuffer
                        .wrap(oram.access("j", Optional.<byte[]>empty(), false).get())
                        .getInt() < _obliviousNextPowerOf2(ByteBuffer
                                .wrap(oram.access("cols", Optional.<byte[]>empty(), false).get()).getInt()); oram
                                        .access("j", Optional.ofNullable(ByteBuffer.allocate(4)
                                                .putInt(ByteBuffer
                                                        .wrap(oram.access("j", Optional.<byte[]>empty(), false).get())
                                                        .getInt() + 1)
                                                .array()), true)) {
                    if (ByteBuffer.wrap(oram.access("j", Optional.<byte[]>empty(), false).get()).getInt() < ByteBuffer
                            .wrap(oram.access("cols", Optional.<byte[]>empty(), false).get()).getInt()) {
                        oram.access("scoreDiag",
                                Optional.ofNullable(
                                        ByteBuffer
                                                .allocate(4).putInt(
                                                        ByteBuffer
                                                                .wrap(oram.access(
                                                                        "H" + "["
                                                                                + String.valueOf(ByteBuffer.wrap(
                                                                                        oram.access("i", Optional
                                                                                                .<byte[]>empty(), false)
                                                                                                .get())
                                                                                        .getInt() - 1)
                                                                                + "]" + "["
                                                                                + String.valueOf(ByteBuffer
                                                                                        .wrap(oram.access(
                                                                                                "j",
                                                                                                Optional.<byte[]>empty(),
                                                                                                false).get())
                                                                                        .getInt() - 1)
                                                                                + "]",
                                                                        Optional.<byte[]>empty(), false).get())
                                                                .getInt()
                                                                + (new String(oram
                                                                        .access("seq1", Optional.<byte[]>empty(), false)
                                                                        .get())
                                                                        .charAt(ByteBuffer
                                                                                .wrap(oram
                                                                                        .access("i", Optional
                                                                                                .<byte[]>empty(), false)
                                                                                        .get())
                                                                                .getInt()
                                                                                - 1) == new String(oram.access(
                                                                                        "seq2",
                                                                                        Optional.<byte[]>empty(), false)
                                                                                        .get())
                                                                                        .charAt(ByteBuffer.wrap(oram
                                                                                                .access("j", Optional
                                                                                                        .<byte[]>empty(),
                                                                                                        false)
                                                                                                .get()).getInt()
                                                                                                - 1) ? ByteBuffer
                                                                                                        .wrap(oram
                                                                                                                .access("matchScore",
                                                                                                                        Optional.<byte[]>empty(),
                                                                                                                        false)
                                                                                                                .get())
                                                                                                        .getInt()
                                                                                                        : ByteBuffer
                                                                                                                .wrap(oram
                                                                                                                        .access("mismatchScore",
                                                                                                                                Optional.<byte[]>empty(),
                                                                                                                                false)
                                                                                                                        .get())
                                                                                                                .getInt()))
                                                .array()),
                                true);
                        oram.access(
                                "scoreUp", Optional
                                        .ofNullable(
                                                ByteBuffer.allocate(4)
                                                        .putInt(ByteBuffer
                                                                .wrap(oram.access(
                                                                        "H" + "["
                                                                                + String.valueOf(ByteBuffer.wrap(oram
                                                                                        .access("i", Optional
                                                                                                .<byte[]>empty(), false)
                                                                                        .get()).getInt() - 1)
                                                                                + "]" + "["
                                                                                + String.valueOf(ByteBuffer.wrap(oram
                                                                                        .access("j", Optional
                                                                                                .<byte[]>empty(), false)
                                                                                        .get()).getInt())
                                                                                + "]",
                                                                        Optional.<byte[]>empty(), false).get())
                                                                .getInt()
                                                                + ByteBuffer
                                                                        .wrap(oram
                                                                                .access("gapPenalty",
                                                                                        Optional.<byte[]>empty(), false)
                                                                                .get())
                                                                        .getInt())
                                                        .array()),
                                true);
                        oram.access(
                                "scoreLeft", Optional
                                        .ofNullable(
                                                ByteBuffer.allocate(4).putInt(ByteBuffer
                                                        .wrap(oram.access(
                                                                "H" + "["
                                                                        + String.valueOf(ByteBuffer.wrap(oram.access(
                                                                                "i", Optional.<byte[]>empty(), false)
                                                                                .get()).getInt())
                                                                        + "]" + "["
                                                                        + String.valueOf(
                                                                                ByteBuffer.wrap(oram
                                                                                        .access("j", Optional
                                                                                                .<byte[]>empty(), false)
                                                                                        .get()).getInt() - 1)
                                                                        + "]",
                                                                Optional.<byte[]>empty(), false).get())
                                                        .getInt()
                                                        + ByteBuffer
                                                                .wrap(oram.access("gapPenalty",
                                                                        Optional.<byte[]>empty(), false).get())
                                                                .getInt())
                                                        .array()),
                                true);
                        oram.access("score",
                                Optional.ofNullable(ByteBuffer.allocate(4).putInt(ByteBuffer
                                        .wrap(oram.access("scoreDiag", Optional.<byte[]>empty(), false).get()).getInt())
                                        .array()),
                                true);
                        if (ByteBuffer.wrap(oram.access("scoreUp", Optional.<byte[]>empty(), false).get())
                                .getInt() > ByteBuffer.wrap(oram.access("score", Optional.<byte[]>empty(), false).get())
                                        .getInt()) {
                            oram.access("score",
                                    Optional.ofNullable(ByteBuffer.allocate(4)
                                            .putInt(ByteBuffer
                                                    .wrap(oram.access("scoreUp", Optional.<byte[]>empty(), false).get())
                                                    .getInt())
                                            .array()),
                                    true);
                        } else {
                        }
                        if (ByteBuffer.wrap(oram.access("scoreLeft", Optional.<byte[]>empty(), false).get())
                                .getInt() > ByteBuffer.wrap(oram.access("score", Optional.<byte[]>empty(), false).get())
                                        .getInt()) {
                            oram.access("score", Optional.ofNullable(ByteBuffer.allocate(4).putInt(ByteBuffer
                                    .wrap(oram.access("scoreLeft", Optional.<byte[]>empty(), false).get()).getInt())
                                    .array()), true);
                        } else {
                        }
                        if (0 > ByteBuffer.wrap(oram.access("score", Optional.<byte[]>empty(), false).get()).getInt()) {
                            oram.access("score", Optional.ofNullable(ByteBuffer.allocate(4).putInt(0).array()), true);
                        } else {
                        }
                        oram.access(
                                "H" + "["
                                        + String.valueOf(ByteBuffer
                                                .wrap(oram.access("i", Optional.<byte[]>empty(), false).get()).getInt())
                                        + "]" + "["
                                        + String.valueOf(ByteBuffer
                                                .wrap(oram.access("j", Optional.<byte[]>empty(), false).get()).getInt())
                                        + "]",
                                Optional.ofNullable(ByteBuffer.allocate(4).putInt(ByteBuffer
                                        .wrap(oram.access("score", Optional.<byte[]>empty(), false).get()).getInt())
                                        .array()),
                                true);
                        if (ByteBuffer.wrap(oram.access("score", Optional.<byte[]>empty(), false).get())
                                .getInt() > ByteBuffer
                                        .wrap(oram.access("maxScore", Optional.<byte[]>empty(), false).get())
                                        .getInt()) {
                            oram.access("maxScore",
                                    Optional.ofNullable(ByteBuffer.allocate(4).putInt(ByteBuffer
                                            .wrap(oram.access("score", Optional.<byte[]>empty(), false).get()).getInt())
                                            .array()),
                                    true);
                            oram.access("maxI",
                                    Optional.ofNullable(ByteBuffer.allocate(4).putInt(ByteBuffer
                                            .wrap(oram.access("i", Optional.<byte[]>empty(), false).get()).getInt())
                                            .array()),
                                    true);
                            oram.access("maxJ",
                                    Optional.ofNullable(ByteBuffer.allocate(4).putInt(ByteBuffer
                                            .wrap(oram.access("j", Optional.<byte[]>empty(), false).get()).getInt())
                                            .array()),
                                    true);
                        } else {
                        }
                    } else {
                        oram.access("dummy", Optional.<byte[]>empty(), false);
                        oram.access("dummy", Optional.<byte[]>empty(), false);
                        oram.access("dummy", Optional.<byte[]>empty(), false);
                        oram.access("dummy", Optional.<byte[]>empty(), false);
                    }
                }
            } else {
                oram.access("dummy", Optional.<byte[]>empty(), false);
                oram.access("dummy", Optional.<byte[]>empty(), false);
                oram.access("dummy", Optional.<byte[]>empty(), false);
                oram.access("dummy", Optional.<byte[]>empty(), false);
                oram.access("dummy", Optional.<byte[]>empty(), false);
                oram.access("dummy", Optional.<byte[]>empty(), false);
                oram.access("dummy", Optional.<byte[]>empty(), false);
                oram.access("dummy", Optional.<byte[]>empty(), false);
            }
        }
        System.out.println("Scoring Matrix:");
        for (oram.access("i", Optional.ofNullable(ByteBuffer.allocate(4).putInt(0).array()), true); ByteBuffer
                .wrap(oram.access("i", Optional.<byte[]>empty(), false).get())
                .getInt() < _obliviousNextPowerOf2(ByteBuffer
                        .wrap(oram.access("rows", Optional.<byte[]>empty(), false).get()).getInt()); oram.access(
                                "i",
                                Optional.ofNullable(ByteBuffer.allocate(4).putInt(ByteBuffer
                                        .wrap(oram.access("i", Optional.<byte[]>empty(), false).get()).getInt() + 1)
                                        .array()),
                                true)) {
            if (ByteBuffer.wrap(oram.access("i", Optional.<byte[]>empty(), false).get()).getInt() < ByteBuffer
                    .wrap(oram.access("rows", Optional.<byte[]>empty(), false).get()).getInt()) {
                for (oram.access("j", Optional.ofNullable(ByteBuffer.allocate(4).putInt(0).array()), true); ByteBuffer
                        .wrap(oram.access("j", Optional.<byte[]>empty(), false).get())
                        .getInt() < _obliviousNextPowerOf2(ByteBuffer
                                .wrap(oram.access("cols", Optional.<byte[]>empty(), false).get()).getInt()); oram
                                        .access("j", Optional.ofNullable(ByteBuffer.allocate(4)
                                                .putInt(ByteBuffer
                                                        .wrap(oram.access("j", Optional.<byte[]>empty(), false).get())
                                                        .getInt() + 1)
                                                .array()), true)) {
                    if (ByteBuffer.wrap(oram.access("j", Optional.<byte[]>empty(), false).get()).getInt() < ByteBuffer
                            .wrap(oram.access("cols", Optional.<byte[]>empty(), false).get()).getInt()) {
                        System.out.printf("%3d ", ByteBuffer.wrap(oram.access(
                                "H" + "["
                                        + String.valueOf(ByteBuffer
                                                .wrap(oram.access("i", Optional.<byte[]>empty(), false).get()).getInt())
                                        + "]" + "["
                                        + String.valueOf(ByteBuffer
                                                .wrap(oram.access("j", Optional.<byte[]>empty(), false).get()).getInt())
                                        + "]",
                                Optional.<byte[]>empty(), false).get()).getInt());
                    } else {
                        oram.access("dummy", Optional.<byte[]>empty(), false);
                    }
                }
                System.out.println();
            } else {
                oram.access("dummy", Optional.<byte[]>empty(), false);
                oram.access("dummy", Optional.<byte[]>empty(), false);
            }
        }
        oram.access("alignedSeq1", Optional.ofNullable(("").getBytes()), true);
        oram.access("alignedSeq2", Optional.ofNullable(("").getBytes()), true);
        oram.access("i", Optional.ofNullable(ByteBuffer.allocate(4)
                .putInt(ByteBuffer.wrap(oram.access("maxI", Optional.<byte[]>empty(), false).get()).getInt()).array()),
                true);
        oram.access("j", Optional.ofNullable(ByteBuffer.allocate(4)
                .putInt(ByteBuffer.wrap(oram.access("maxJ", Optional.<byte[]>empty(), false).get()).getInt()).array()),
                true);
        while (ByteBuffer.wrap(oram.access("i", Optional.<byte[]>empty(), false).get()).getInt() > 0
                && ByteBuffer.wrap(oram.access("j", Optional.<byte[]>empty(), false).get()).getInt() > 0
                && ByteBuffer.wrap(oram.access(
                        "H" + "["
                                + String.valueOf(ByteBuffer
                                        .wrap(oram.access("i", Optional.<byte[]>empty(), false).get()).getInt())
                                + "]" + "["
                                + String.valueOf(ByteBuffer
                                        .wrap(oram.access("j", Optional.<byte[]>empty(), false).get()).getInt())
                                + "]",
                        Optional.<byte[]>empty(), false).get()).getInt() != 0) {
            if (ByteBuffer.wrap(oram.access("i", Optional.<byte[]>empty(), false).get()).getInt() > 0
                    && ByteBuffer.wrap(
                            oram.access("j", Optional.<byte[]>empty(), false).get()).getInt() > 0
                    && (ByteBuffer
                            .wrap(oram.access("H" + "[" + String
                                    .valueOf(ByteBuffer.wrap(oram.access("i", Optional.<byte[]>empty(), false).get())
                                            .getInt())
                                    + "]" + "["
                                    + String.valueOf(ByteBuffer
                                            .wrap(oram.access("j", Optional.<byte[]>empty(), false).get()).getInt())
                                    + "]", Optional.<byte[]>empty(), false).get())
                            .getInt() == ByteBuffer.wrap(oram.access("H" + "["
                                    + String.valueOf(ByteBuffer
                                            .wrap(oram.access("i", Optional.<byte[]>empty(), false).get()).getInt() - 1)
                                    + "]" + "["
                                    + String.valueOf(ByteBuffer
                                            .wrap(oram.access("j", Optional.<byte[]>empty(), false).get()).getInt() - 1)
                                    + "]", Optional.<byte[]>empty(), false).get()).getInt()
                                    + (new String(oram.access("seq1", Optional.<byte[]>empty(), false).get()).charAt(
                                            ByteBuffer.wrap(oram.access("i", Optional.<byte[]>empty(), false).get())
                                                    .getInt()
                                                    - 1) == new String(oram.access("seq2", Optional.<byte[]>empty(),
                                                            false).get()).charAt(
                                                                    ByteBuffer
                                                                            .wrap(oram
                                                                                    .access("j", Optional
                                                                                            .<byte[]>empty(), false)
                                                                                    .get())
                                                                            .getInt() - 1) ? ByteBuffer
                                                                                    .wrap(oram.access("matchScore",
                                                                                            Optional.<byte[]>empty(),
                                                                                            false).get())
                                                                                    .getInt()
                                                                                    : ByteBuffer.wrap(oram.access(
                                                                                            "mismatchScore",
                                                                                            Optional.<byte[]>empty(),
                                                                                            false).get()).getInt()))) {
                oram.access("alignedSeq1", Optional
                        .ofNullable((new String(oram.access("seq1", Optional.<byte[]>empty(), false).get()).charAt(
                                ByteBuffer.wrap(oram.access("i", Optional.<byte[]>empty(), false).get()).getInt() - 1)
                                + new String(oram.access("alignedSeq1", Optional.<byte[]>empty(), false).get()))
                                .getBytes()),
                        true);
                oram.access("alignedSeq2", Optional
                        .ofNullable((new String(oram.access("seq2", Optional.<byte[]>empty(), false).get()).charAt(
                                ByteBuffer.wrap(oram.access("j", Optional.<byte[]>empty(), false).get()).getInt() - 1)
                                + new String(oram.access("alignedSeq2", Optional.<byte[]>empty(), false).get()))
                                .getBytes()),
                        true);
                oram.access("i",
                        Optional.ofNullable(ByteBuffer.allocate(4).putInt(
                                ByteBuffer.wrap(oram.access("i", Optional.<byte[]>empty(), false).get()).getInt() - 1)
                                .array()),
                        true);
                oram.access("j",
                        Optional.ofNullable(ByteBuffer.allocate(4).putInt(
                                ByteBuffer.wrap(oram.access("j", Optional.<byte[]>empty(), false).get()).getInt() - 1)
                                .array()),
                        true);
            } else // If the score came from an upward move, insert a gap in seq2
            if (ByteBuffer.wrap(oram.access("i", Optional.<byte[]>empty(), false).get()).getInt() > 0
                    && (ByteBuffer
                            .wrap(oram.access("H" + "["
                                    + String.valueOf(ByteBuffer
                                            .wrap(oram.access("i", Optional.<byte[]>empty(), false).get()).getInt())
                                    + "]" + "["
                                    + String.valueOf(ByteBuffer
                                            .wrap(oram.access("j", Optional.<byte[]>empty(), false).get()).getInt())
                                    + "]", Optional.<byte[]>empty(), false).get())
                            .getInt() == ByteBuffer.wrap(oram.access("H" + "["
                                    + String.valueOf(ByteBuffer
                                            .wrap(oram.access("i", Optional.<byte[]>empty(), false).get()).getInt() - 1)
                                    + "]" + "["
                                    + String.valueOf(ByteBuffer
                                            .wrap(oram.access("j", Optional.<byte[]>empty(), false).get()).getInt())
                                    + "]", Optional.<byte[]>empty(), false).get()).getInt()
                                    + ByteBuffer.wrap(oram.access("gapPenalty", Optional.<byte[]>empty(), false).get())
                                            .getInt())) {
                oram.access("alignedSeq1", Optional
                        .ofNullable((new String(oram.access("seq1", Optional.<byte[]>empty(), false).get()).charAt(
                                ByteBuffer.wrap(oram.access("i", Optional.<byte[]>empty(), false).get()).getInt() - 1)
                                + new String(oram.access("alignedSeq1", Optional.<byte[]>empty(), false).get()))
                                .getBytes()),
                        true);
                oram.access("alignedSeq2",
                        Optional.ofNullable(
                                ("-" + new String(oram.access("alignedSeq2", Optional.<byte[]>empty(), false).get()))
                                        .getBytes()),
                        true);
                oram.access("i",
                        Optional.ofNullable(ByteBuffer.allocate(4).putInt(
                                ByteBuffer.wrap(oram.access("i", Optional.<byte[]>empty(), false).get()).getInt() - 1)
                                .array()),
                        true);
            } else // If the score came from a left move, insert a gap in seq1
            if (ByteBuffer.wrap(oram.access("j", Optional.<byte[]>empty(), false).get()).getInt() > 0
                    && (ByteBuffer.wrap(oram.access("H" + "["
                            + String.valueOf(
                                    ByteBuffer.wrap(oram.access("i", Optional.<byte[]>empty(), false).get()).getInt())
                            + "]" + "[" + String
                                    .valueOf(ByteBuffer.wrap(oram.access("j", Optional.<byte[]>empty(), false).get())
                                            .getInt())
                            + "]", Optional.<byte[]>empty(), false).get())
                            .getInt() == ByteBuffer
                                    .wrap(oram.access("H" + "["
                                            + String.valueOf(ByteBuffer
                                                    .wrap(oram.access("i", Optional.<byte[]>empty(), false).get())
                                                    .getInt())
                                            + "]" + "["
                                            + String.valueOf(ByteBuffer
                                                    .wrap(oram.access("j", Optional.<byte[]>empty(), false).get())
                                                    .getInt() - 1)
                                            + "]", Optional.<byte[]>empty(), false).get())
                                    .getInt()
                                    + ByteBuffer.wrap(oram.access("gapPenalty", Optional.<byte[]>empty(), false).get())
                                            .getInt())) {
                oram.access("alignedSeq1",
                        Optional.ofNullable(
                                ("-" + new String(oram.access("alignedSeq1", Optional.<byte[]>empty(), false).get()))
                                        .getBytes()),
                        true);
                oram.access("alignedSeq2", Optional
                        .ofNullable((new String(oram.access("seq2", Optional.<byte[]>empty(), false).get()).charAt(
                                ByteBuffer.wrap(oram.access("j", Optional.<byte[]>empty(), false).get()).getInt() - 1)
                                + new String(oram.access("alignedSeq2", Optional.<byte[]>empty(), false).get()))
                                .getBytes()),
                        true);
                oram.access("j",
                        Optional.ofNullable(ByteBuffer.allocate(4).putInt(
                                ByteBuffer.wrap(oram.access("j", Optional.<byte[]>empty(), false).get()).getInt() - 1)
                                .array()),
                        true);
            } else {
                break;
            }
        }
        // Output the optimal local alignment score and the aligned sequences
        System.out.println("\nOptimal Local Alignment Score: "
                + ByteBuffer.wrap(oram.access("maxScore", Optional.<byte[]>empty(), false).get()).getInt());
        System.out.println(
                "Aligned Sequence 1: " + new String(oram.access("alignedSeq1", Optional.<byte[]>empty(), false).get()));
        System.out.println(
                "Aligned Sequence 2: " + new String(oram.access("alignedSeq2", Optional.<byte[]>empty(), false).get()));
    }

    private static int _obliviousNextPowerOf2(int n) {
        if (n < 1) {
            return 1;
        } else {
        }
        n--;
        n |= n >> 1;
        n |= n >> 2;
        n |= n >> 4;
        n |= n >> 8;
        n |= n >> 16;
        return n + 1;
    }
}

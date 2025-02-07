package com.example;

public class SmithWaterman {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java SmithWaterman <sequence1> <sequence2>");
            System.exit(1);
        }

        String seq1 = args[0];
        String seq2 = args[1];

        // Scoring scheme parameters
        int gapPenalty = -2;
        int matchScore = 2;
        int mismatchScore = -1;

        int rows = seq1.length() + 1;
        int cols = seq2.length() + 1;
        int[][] H = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                H[i][j] = 0;
            }
        }

        int maxScore = 0, maxI = 0, maxJ = 0;

        for (int i = 1; i < rows; i++) {
            for (int j = 1; j < cols; j++) {
                int scoreDiag = H[i - 1][j - 1]
                        + (seq1.charAt(i - 1) == seq2.charAt(j - 1) ? matchScore : mismatchScore);
                int scoreUp = H[i - 1][j] + gapPenalty;
                int scoreLeft = H[i][j - 1] + gapPenalty;

                int score = scoreDiag;
                if (scoreUp > score) {
                    score = scoreUp;
                }
                if (scoreLeft > score) {
                    score = scoreLeft;
                }
                if (0 > score) {
                    score = 0;
                }

                H[i][j] = score;

                if (score > maxScore) {
                    maxScore = score;
                    maxI = i;
                    maxJ = j;
                }
            }
        }

        System.out.println("Scoring Matrix:");
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                System.out.printf("%3d ", H[i][j]);
            }
            System.out.println();
        }

        String alignedSeq1 = "";
        String alignedSeq2 = "";
        int i = maxI, j = maxJ;
        while (i > 0 && j > 0 && H[i][j] != 0) {
            if (i > 0 && j > 0
                    && (H[i][j] == H[i - 1][j - 1] + (seq1.charAt(i - 1) == seq2.charAt(j - 1) ? matchScore : mismatchScore))) {
                alignedSeq1 = seq1.charAt(i - 1) + alignedSeq1;
                alignedSeq2 = seq2.charAt(j - 1) + alignedSeq2;
                i = i - 1;
                j = j - 1;
            } // If the score came from an upward move, insert a gap in seq2
            else if (i > 0 && (H[i][j] == H[i - 1][j] + gapPenalty)) {
                alignedSeq1 = seq1.charAt(i - 1) + alignedSeq1;
                alignedSeq2 = "-" + alignedSeq2;
                i = i - 1;
            } // If the score came from a left move, insert a gap in seq1
            else if (j > 0 && (H[i][j] == H[i][j - 1] + gapPenalty)) {
                alignedSeq1 = "-" + alignedSeq1;
                alignedSeq2 = seq2.charAt(j - 1) + alignedSeq2;
                j = j - 1;
            } else {
                break;
            }
        }

        // Output the optimal local alignment score and the aligned sequences
        System.out.println("\nOptimal Local Alignment Score: " + maxScore);
        System.out.println("Aligned Sequence 1: " + alignedSeq1);
        System.out.println("Aligned Sequence 2: " + alignedSeq2);
    }
}

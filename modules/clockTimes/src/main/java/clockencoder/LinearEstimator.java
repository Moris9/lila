package org.lila.clockencoder;

public class LinearEstimator {
    // Input: Array of absolute clock times for a players move
    // Output: Encoded array of clock times.
    public static int[] encode(int[] src) {
       return encodeDecode(src, true);
    }

    public static int[] decode(int[] src) {
        return encodeDecode(src, false);
    }

    private static int[] encodeDecode(int[] src, boolean isEncoding) {
        int size = src.length;
        int[] dest = new int[size];
        dest[0] = src[0];
        dest[size - 1] = src[size - 1];

        int[] realValues = isEncoding ? src : dest;
        encodeHelper(src, dest, realValues, 0, size - 1);

        return dest;
    }

    private static void encodeHelper(int[] src, int[] dest, int[] realValues,
                                     int startIdx, int endIdx) {
        int l = endIdx - startIdx;
        if (l == 0) return;

        int midIdx = startIdx + l / 2;

        // It's important to save estimate in fixed precision to ensure
        // the encode and decode math behaves identically.
        int estimate = (realValues[startIdx] * (endIdx - midIdx) +
                        realValues[endIdx]   * (midIdx - startIdx)) / l;

        dest[midIdx] = estimate - src[midIdx];

        encodeHelper(src, dest, realValues, startIdx, midIdx);
        encodeHelper(src, dest, realValues, midIdx, endIdx);
    }
}
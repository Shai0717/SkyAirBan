package net.shai0717.skyairban;

import java.util.Arrays;

public final class SkyAirTest {
    public static void main(String[] args) {
        int min = -2;
        int max = 4;
        boolean[] air = new boolean[(max - min) * 256];
        set(air, min, 1, 3, 1);
        set(air, min, 1, 2, 1);
        set(air, min, 2, 2, 1);
        set(air, min, 2, 1, 1);
        set(air, min, 3, 1, 1);
        set(air, min, 4, 1, 1);
        set(air, min, 4, 2, 1);
        set(air, min, 8, 0, 8);
        set(air, min, 0, 0, 1);
        byte[] data = SkyAir.capture(min, max, (x, y, z) -> air[index(min, x, y, z)]);
        require(SkyAir.valid(data, min, max), "valid data");
        require(contains(data, min, max, 1, 3, 1), "top seed");
        require(contains(data, min, max, 2, 1, 1), "six-direction path");
        require(contains(data, min, max, 4, 2, 1), "path can rise under an overhang");
        require(!contains(data, min, max, 8, 0, 8), "sealed cave");
        require(!contains(data, min, max, 0, 0, 1), "chunk edge does not wrap");
        require(!contains(data, min, max, 1, 0, 1), "solid block");
        require(!contains(data, min, max, 1, max, 1), "outside height");
        set(air, min, 1, 0, 1);
        require(!contains(data, min, max, 1, 0, 1), "mining does not modify captured data");
        air[index(min, 1, 2, 1)] = false;
        require(contains(data, min, max, 2, 1, 1), "placement does not modify captured data");
        require(!SkyAir.valid(data, min + 1, max), "height mismatch");
        require(!SkyAir.valid(Arrays.copyOf(data, data.length - 1), min, max), "truncated data");

        byte[] allAir = SkyAir.capture(min, max, (x, y, z) -> true);
        require(contains(allAir, min, max, 15, min, 15), "all-air bottom corner");
        require(contains(allAir, min, max, -1, max - 1, -1), "negative world coordinates");
        byte[] noSky = SkyAir.capture(min, max, (x, y, z) -> y != max - 1);
        require(!contains(noSky, min, max, 0, min, 0), "covered top");
        System.out.println("SkyAir tests passed");
    }

    private static int index(int min, int x, int y, int z) {
        return (y - min) * 256 + z * 16 + x;
    }

    private static void set(boolean[] air, int min, int x, int y, int z) {
        air[index(min, x, y, z)] = true;
    }

    private static boolean contains(byte[] data, int min, int max, int x, int y, int z) {
        return SkyAir.contains(data, min, max, x, y, z);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

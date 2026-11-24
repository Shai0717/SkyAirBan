package net.shai0717.skyairban;

import java.nio.ByteBuffer;
import java.util.BitSet;

final class SkyAir {
    private static final int HEADER_SIZE = Integer.BYTES * 2;

    @FunctionalInterface
    interface AirTest {
        boolean isAir(int x, int y, int z);
    }

    private SkyAir() {
    }

    static byte[] capture(int minY, int maxY, AirTest air) {
        int height = Math.subtractExact(maxY, minY);
        if (height <= 0) {
            throw new IllegalArgumentException("Invalid world height");
        }
        int count = Math.multiplyExact(height, 256);
        byte[] result = new byte[Math.addExact(HEADER_SIZE, count / 8)];
        ByteBuffer.wrap(result).putInt(minY).putInt(maxY);
        BitSet visited = new BitSet(count);
        int[] queue = new int[count];
        int head = 0;
        int tail = 0;

        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int index = (height - 1) * 256 + z * 16 + x;
                visited.set(index);
                if (air.isAir(x, maxY - 1, z)) {
                    queue[tail++] = index;
                }
            }
        }

        while (head < tail) {
            int index = queue[head++];
            result[HEADER_SIZE + index / 8] |= (byte) (1 << (index & 7));
            int x = index & 15;
            int z = (index >>> 4) & 15;
            int y = index >>> 8;
            if (x > 0) tail = visit(index - 1, minY, air, visited, queue, tail);
            if (x < 15) tail = visit(index + 1, minY, air, visited, queue, tail);
            if (z > 0) tail = visit(index - 16, minY, air, visited, queue, tail);
            if (z < 15) tail = visit(index + 16, minY, air, visited, queue, tail);
            if (y > 0) tail = visit(index - 256, minY, air, visited, queue, tail);
            if (y < height - 1) tail = visit(index + 256, minY, air, visited, queue, tail);
        }
        return result;
    }

    private static int visit(int index, int minY, AirTest air, BitSet visited, int[] queue, int tail) {
        if (!visited.get(index)) {
            visited.set(index);
            if (air.isAir(index & 15, minY + (index >>> 8), (index >>> 4) & 15)) {
                queue[tail++] = index;
            }
        }
        return tail;
    }

    static boolean valid(byte[] data, int minY, int maxY) {
        long height = (long) maxY - minY;
        if (data.length < HEADER_SIZE || height <= 0 || HEADER_SIZE + height * 32 != data.length) {
            return false;
        }
        ByteBuffer header = ByteBuffer.wrap(data);
        return header.getInt() == minY && header.getInt() == maxY;
    }

    static boolean contains(byte[] data, int minY, int maxY, int x, int y, int z) {
        if (y < minY || y >= maxY) {
            return false;
        }
        int index = (y - minY) * 256 + (z & 15) * 16 + (x & 15);
        return (data[HEADER_SIZE + index / 8] & (1 << (index & 7))) != 0;
    }
}

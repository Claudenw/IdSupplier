package org.example;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class ShortSupplier {
    private volatile long index;
    private volatile long increment;
    private volatile long count;
    private short[] buffer;
    private volatile int limit;
    private static final int MAX_LIMIT = 0x10000;
    private static final short[] FILLER;

    static {
        FILLER = new short[MAX_LIMIT];
        for (int i = 1; i < MAX_LIMIT; i++) {
            FILLER[i] = (short) i;
        }
    }

    public ShortSupplier() {
        Random r = new Random();
        index = r.nextLong();
        increment = r.nextLong();
        count = 1;
        buffer = new short[MAX_LIMIT];
        resetBuffer();
    }

    private void resetBuffer() {
        System.arraycopy(FILLER, 0, buffer, 0, MAX_LIMIT);
        limit = MAX_LIMIT;
    }

    /**
     * Performs a modulus calculation on an unsigned long and an integer divisor.
     * 
     * @param dividend a unsigned long value to calculate the modulus of.
     * @param divisor the divisor for the modulus calculation.
     * @return the remainder or modulus value.
     */
    public static int mod(final long dividend, final int divisor) {
        // See Hacker's Delight (2nd ed), section 9.3.
        // Assume divisor is positive.
        // Divide half the unsigned number and then double the quotient result.
        final long quotient = (dividend >>> 1) / divisor << 1;
        final long remainder = dividend - quotient * divisor;
        // remainder in [0, 2 * divisor)
        return (int) (remainder >= divisor ? remainder - divisor : remainder);
    }

    public short nextId() {
        int idx = mod(index, limit--);
     // Update index and handle wrapping
        index -= increment;

        // Incorporate the counter into the increment to create a
        // tetrahedral number additional term, and handle wrapping.
        increment -= count;
        short result = buffer[idx];
        if (limit == 0) {
            resetBuffer();
        } else {
            int src = idx + 1;
            int len = MAX_LIMIT - src;
            System.arraycopy(buffer, src, buffer, idx, len);
        }
        return result;
    }

    // test code
    public static void main(String[] args) {
        Set<Short> set = new HashSet<>();
        int collisionCount = 0;
        ShortSupplier ss = new ShortSupplier();
        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < MAX_LIMIT; i++) {
                Short s = ss.nextId();
                if (!set.add(s)) {
                    System.err.println("Collision at " + i + " value " + s);
                    collisionCount++;
                }
            }
            System.out.println(collisionCount + " Collisions");
            set.clear();
        }
        System.out.println("DONE");
    }
}

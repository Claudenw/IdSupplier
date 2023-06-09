package org.example;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class ShortSupplier {
    private static final int FILLER_LIMIT = 0x10000;
    private static final short[] FILLER;

    private final Hasher hasher;
    private short[] buffer;
    private volatile int limit;
    private final int fillerOffset;

    static {
        FILLER = new short[FILLER_LIMIT];
        for (int i = 1; i < FILLER_LIMIT; i++) {
            FILLER[i] = (short) i;
        }
    }

    /**
     * Returns shorts over the entire range (0x0, 0xFFFF).
     */
    public ShortSupplier() {
        this(0, FILLER_LIMIT);
    }

    /**
     * 
     * @param start integer value in the range [0x0, 0xFFFF) lowest value returned (inclusive).
     * @param end integer in the range (start+1, 0xFFFF] highest value returned (exclusive)
     */
    public ShortSupplier(int start, int end) {
        if (start < 0 || start > FILLER_LIMIT-1) {
            throw new IllegalArgumentException( "start must be in the range [0x0, 0xFFFF)");
        }
        if (end<=start || end > FILLER_LIMIT) {
            throw new IllegalArgumentException( "start must be in the range (start+1, 0xFFFF]");
        }
        hasher = new Hasher();
        buffer = new short[end - start];
        this.fillerOffset = start;
        resetBuffer();
    }

    private void resetBuffer() {
        System.arraycopy(FILLER, fillerOffset, buffer, 0, buffer.length);
        limit = buffer.length;
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

    /**
     * Gets the next ID from the list.
     * @return the next ID from the list.
     */
    public short nextId() {
        int idx = hasher.nextIdx();
        short result = buffer[idx];
        if (limit == 0) {
            resetBuffer();
        } else {
            int src = idx + 1;
            if (src < buffer.length) { 
                int len = buffer.length - src;
                System.arraycopy(buffer, src, buffer, idx, len);
            }
        }
        return result;
    }
    
    /**
     * Returns true if no values have been returned yet or if the values have just been reset.
     * @return
     */
    public boolean atStart() {
        return limit == buffer.length;
    }

    private class Hasher {
        private volatile long index;
        private volatile long increment;
        private volatile long count;

        Hasher() {
            Random r = new Random();
            index = r.nextLong();
            increment = r.nextLong();
            count = 1;
        }

        int nextIdx() {
            int idx = mod(index, limit--);
            // Update index and handle wrapping
            index -= increment;

            // Incorporate the counter into the increment to create a
            // tetrahedral number additional term, and handle wrapping.
            increment -= count;
            return idx;
        }
    }
    

    // test code
    public static void main(String[] args) {
        Set<Short> set = new HashSet<>();
        int collisionCount = 0;
        ShortSupplier ss = new ShortSupplier();
        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < FILLER_LIMIT; i++) {
                Short s = ss.nextId();
                if (!set.add(s)) {
                    System.err.println("Collision at " + i + " value " + s);
                    collisionCount++;
                }
            }
            System.out.println(collisionCount + " Collisions");
            set.clear();
        }
        
        // test partial
        System.out.println( "Starting partial test");
        ss = new ShortSupplier( 0x0F, 0x5F );
        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < ss.buffer.length; i++) {
                Short s = ss.nextId();
                if (s<0xF || 0x5F <= s) {
                    System.err.println( "Invalid value: "+s);
                }
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

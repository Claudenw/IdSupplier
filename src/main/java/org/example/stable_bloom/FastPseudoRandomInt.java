package org.example.stable_bloom;

import java.util.Random;

import org.apache.commons.collections4.bloomfilter.BitMap;

/**
 * Generate sudo random integers using combinatorial hashing as described by
 * <a href="https://www.eecs.harvard.edu/~michaelm/postscripts/tr-02-05.pdf">Krisch and Mitzenmacher</a> 
 * using the enhanced double hashing technique described in the wikipedia article  
 * <a href="https://en.wikipedia.org/wiki/Double_hashing#Enhanced_double_hashing">Double Hashing</a> and random seeds
 * for the initial value and the increment.
 */
public class FastPseudoRandomInt {
    private volatile long index;
    private volatile long increment;
    private volatile long count;

    public FastPseudoRandomInt() {
        Random r = new Random();
        index = r.nextLong();
        increment = r.nextLong();
        count = 1;
    }

    /**
     * Generates a sudo random number in the range [0,limit).
     * @param limit The limit for the index value (exclusive).
     * @return a pseudo random integer.
     */
    public int nextInt(int limit) {
        int idx = BitMap.mod(index, limit);
        // Update index and handle wrapping
        index -= increment;

        // Incorporate the counter into the increment to create a
        // tetrahedral number additional term, and handle wrapping.
        increment -= count;
        return idx;
    }

}

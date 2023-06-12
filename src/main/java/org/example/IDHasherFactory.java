package org.example;

import org.apache.commons.codec.digest.MurmurHash3;
import org.apache.commons.collections4.bloomfilter.EnhancedDoubleHasher;
import org.apache.commons.collections4.bloomfilter.Hasher;

public class IDHasherFactory {
    private static final long HASHERS[][];
    public static final int LIMIT = 0x10000;
    static {
        HASHERS = new long[LIMIT][2];
        byte[] bytes = new byte[2];
        for (int i = 0; i < LIMIT; i++) {
            HASHERS[i] = MurmurHash3.hash128(bytes);
            bytes[1]++;
            if (bytes[1] == 0) {
                bytes[0]++;
            }
        }
    }

    public static Hasher get(short id) {
        int idx = (0xFFFF) & id;
        return new EnhancedDoubleHasher(HASHERS[idx][0], HASHERS[idx][1]);
    }

    private IDHasherFactory() {
    }

}

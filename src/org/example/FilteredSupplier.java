package org.example;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.bloomfilter.Hasher;
import org.example.stable_bloom.StableBloomFilter;

public class FilteredSupplier {
    private StableBloomFilter stableBloomFilter;
    private ShortSupplier shortSupplier;

    
    public FilteredSupplier() {
        stableBloomFilter = new StableBloomFilter(StableBloomFilter.StableShape.byMaxElements(IDHasherFactory.LIMIT));
        shortSupplier = new ShortSupplier();
    }
    
    public short nextId() {
        short result;
        Hasher hasher;
        do {
            result = shortSupplier.nextId();
            hasher = IDHasherFactory.get( result );            
        } while (stableBloomFilter.contains(hasher));
        stableBloomFilter.merge( hasher );
        return result;
    }
    
    public void saw( short result ) {
        stableBloomFilter.merge(IDHasherFactory.get( result ));
    }

    public static void main(String[] args) {
        Set<Short> set = new HashSet<>();
        int collisionCount = 0;
        FilteredSupplier supplier = new FilteredSupplier();
        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < IDHasherFactory.LIMIT; i++) {
                Short s = supplier.nextId();
                if (!set.add(s)) {
                    collisionCount++;
                }
            }
            System.out.println(collisionCount + " Collisions");
            set.clear();
        }
        System.out.println("DONE");
    }
}

package org.example.stable_bloom;

import java.util.Objects;
import java.util.function.LongPredicate;

import org.apache.commons.collections4.bloomfilter.BitCountProducer;
import org.apache.commons.collections4.bloomfilter.BitMap;
import org.apache.commons.collections4.bloomfilter.CountingBloomFilter;
import org.apache.commons.collections4.bloomfilter.IndexProducer;
import org.apache.commons.collections4.bloomfilter.Shape;

/**
 * Based http://webdocs.cs.ualberta.ca/~drafiei/papers/DupDet06Sigmod.pdf
 *
 */
public class StableBloomFilter implements CountingBloomFilter {
    private final StableShape shape;
    private final FastPseudoRandomInt idxFactory;
    private final BufferManager buffer;

    public StableBloomFilter(StableShape shape) {
        this.shape = shape;
        this.idxFactory = new FastPseudoRandomInt();
        this.buffer = AbstractBufferManager.instance(shape);
    }

    public StableShape getStableShape() {
        return shape;
    }

    @Override
    public int characteristics() {
        return SPARSE;
    }

    @Override
    public Shape getShape() {
        return shape.getShape();
    }

    @Override
    public void clear() {
        buffer.clear();
    }

    @Override
    public boolean contains(IndexProducer indexProducer) {
        boolean[] duplicateFlag = { false };
        indexProducer.forEachIndex(x -> {
            duplicateFlag[0] |= (x == 0);
            return true;
        });
        return duplicateFlag[0];
    }

    @Override
    public int cardinality() {
        int result = 0;
        for (int i = 0; i < shape.getNumberOfEntries(); i++) {
            if (buffer.isSet(i)) {
                result++;
            }
        }
        return result;
    }

    @Override
    public boolean merge(final IndexProducer indexProducer) {
        decrement();
        return indexProducer.forEachIndex(x -> {
            if (x > shape.getNumberOfEntries()) {
                return false;
            }
            buffer.set(x);
            return true;
        });
    }

    @Override
    public boolean forEachBitMap(LongPredicate consumer) {
        Objects.requireNonNull(consumer, "consumer");
        final int blocksm1 = BitMap.numberOfBitMaps(shape.getNumberOfEntries()) - 1;
        int i = 0;
        long value;
        // must break final block separate as the number of bits may not fall on the
        // long boundary
        for (int j = 0; j < blocksm1; j++) {
            value = 0;
            for (int k = 0; k < Long.SIZE; k++) {
                if (buffer.isSet(i++)) {
                    value |= BitMap.getLongBit(k);
                }
            }
            if (!consumer.test(value)) {
                return false;
            }
        }
        // Final block
        value = 0;
        for (int k = 0; i < shape.getNumberOfEntries(); k++) {
            if (buffer.isSet(i++)) {
                value |= BitMap.getLongBit(k);
            }
        }
        return consumer.test(value);

    }

    @Override
    public boolean forEachCount(BitCountConsumer consumer) {
        for (int i = 0; i < shape.getNumberOfEntries(); i++) {
            int b = buffer.get(i);
            if (b != 0) {
                if (consumer.test(i, b)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean add(BitCountProducer other) {
        return other.forEachCount((x, y) -> {
            if (x > shape.getNumberOfEntries()) {
                return false;
            }
            buffer.func(x, y, (x1, y1) -> x1 + y1);
            return true;
        });
    }

    @Override
    public boolean subtract(BitCountProducer other) {
        return other.forEachCount((x, y) -> {
            if (x > shape.getNumberOfEntries()) {
                return false;
            }
            buffer.func(x, y, (x1, y1) -> x1 - y1);
            return true;
        });
    }

    @Override
    public CountingBloomFilter copy() {
        StableBloomFilter result = new StableBloomFilter(this.shape);
        forEachCount((x, y) -> {
            result.buffer.func(x, y, (x1, y1) -> y1);
            return true;
        });
        return result;
    }

    private void decrement() {
        for (int i = 0; i < shape.numberOfCellsDecremented; i++) {
            buffer.decrement(idxFactory.nextInt(shape.getNumberOfEntries()));
        }
    }
}

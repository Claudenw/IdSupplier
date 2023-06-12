package org.example.stable_bloom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.collections4.bloomfilter.AbstractBloomFilterTest;
import org.apache.commons.collections4.bloomfilter.BitMapProducer;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.IncrementingHasher;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.apache.commons.collections4.bloomfilter.TestingHashers;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link StableBloomFilter}.
 */
public class StableBloomFilterTest extends AbstractBloomFilterTest<StableBloomFilter> {

    @Override
    protected StableBloomFilter createEmptyFilter(final Shape shape) {
        StableShape stableShape = StableShape.byShape(shape);
        return new StableBloomFilter(stableShape);
    }

    @Test
    public void testMergeShortBitMapProducer() {
        StableBloomFilter filter = createEmptyFilter(getTestShape());
        // create a producer that returns too few values
        // shape expects 2 longs we are sending 1.
        BitMapProducer producer = p -> {
            return p.test(2L);
        };
        assertTrue(filter.merge(producer));
        assertEquals(1, filter.cardinality());
    }

    /**
     * Tests that the estimated intersection calculations are correct.
     */
    @Override
    @Test
    public final void testEstimateIntersection() {

        final BloomFilter bf = createFilter(getTestShape(), TestingHashers.FROM1);
        final BloomFilter bf2 = TestingHashers.populateFromHashersFrom1AndFrom11(createEmptyFilter(getTestShape()));

        final BloomFilter bf3 = TestingHashers.populateEntireFilter(createEmptyFilter(getTestShape()));

        assertEquals(1, bf.estimateIntersection(bf2));
        assertEquals(1, bf2.estimateIntersection(bf));
        assertEquals(0, bf.estimateIntersection(bf3));
        assertEquals(1, bf2.estimateIntersection(bf));
        assertEquals(0, bf3.estimateIntersection(bf2));

        final BloomFilter bf4 = createEmptyFilter(getTestShape());

        assertEquals(0, bf.estimateIntersection(bf4));
        assertEquals(0, bf4.estimateIntersection(bf));

        BloomFilter bf5 = TestingHashers.mergeHashers(createEmptyFilter(getTestShape()),
                new IncrementingHasher(0, 1)/* 0-16 */, new IncrementingHasher(17, 1)/* 17-33 */,
                new IncrementingHasher(33, 1)/* 33-49 */);
        BloomFilter bf6 = TestingHashers.mergeHashers(createEmptyFilter(getTestShape()),
                new IncrementingHasher(50, 1)/* 50-66 */, new IncrementingHasher(67, 1)/* 67-83 */);
        assertThrows(IllegalArgumentException.class, () -> bf5.estimateIntersection(bf6));

        // infinite with infinite
        assertEquals(bf3.estimateN(), bf3.estimateIntersection(bf3));
    }

    /**
     * Tests that the estimateUnion calculations are correct.
     */
    @Override
    @Test
    public final void testEstimateUnion() {
        final BloomFilter bf = createFilter(getTestShape(), TestingHashers.FROM1);
        final BloomFilter bf2 = createFilter(getTestShape(), TestingHashers.FROM11);

        assertEquals(2, bf.estimateUnion(bf2));
        assertEquals(2, bf2.estimateUnion(bf));

        final BloomFilter bf3 = createEmptyFilter(getTestShape());

        assertEquals(1, bf.estimateUnion(bf3));
        assertEquals(1, bf3.estimateUnion(bf));
    }

    /**
     * Tests that the size estimate is correctly calculated.
     */
    @Override
    @Test
    public final void testEstimateN() {
        // build a filter
        StableBloomFilter filter1 = createFilter(getTestShape(), TestingHashers.FROM1);
        assertEquals(1, filter1.estimateN());

        // the data provided above do not generate an estimate that is equivalent to the
        // actual.
        filter1.merge(new IncrementingHasher(4, 1));
        assertEquals(1, filter1.estimateN());

        filter1.merge(new IncrementingHasher(17, 1));

        assertEquals(2.5, filter1.estimateN(), 0.5);

        filter1 = TestingHashers.populateEntireFilter(createEmptyFilter(getTestShape()));
        assertEquals(7.0, filter1.estimateN(), 1.0);
    }

    /**
     * Tests that isFull() returns the proper values.
     */
    @Override
    @Test
    public final void testIsFull() {

        // create empty filter
        StableBloomFilter filter = createEmptyFilter(getTestShape());
        assertFalse(filter.isFull(), "Should not be full");

        filter = TestingHashers.populateEntireFilter(filter);
        assertFalse(filter.isFull(), "Should not be full");

        filter = createFilter(getTestShape(), new IncrementingHasher(1, 3));
        assertFalse(filter.isFull(), "Should not be full");
    }
}

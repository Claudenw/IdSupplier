package org.example.stable_bloom;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Random;
import java.util.function.LongPredicate;

import org.apache.commons.collections4.bloomfilter.BitCountProducer;
import org.apache.commons.collections4.bloomfilter.BitMap;
import org.apache.commons.collections4.bloomfilter.CountingBloomFilter;
import org.apache.commons.collections4.bloomfilter.IndexProducer;
import org.apache.commons.collections4.bloomfilter.Shape;

/**
 * Based  http://webdocs.cs.ualberta.ca/~drafiei/papers/DupDet06Sigmod.pdf
 *
 */
public class StableBloomFilter implements CountingBloomFilter {

    private ByteBuffer buffer;
    private StableShape shape;
    private volatile long index;
    private volatile long increment;
    private volatile long count;
    private final int resetCount;

    
    public static class StableShape {
        private Shape shape;
        public final int resetValue;
        public final int maxElements;
        public final int numberOfCellsDecremented;

        public static StableShape byMaxElements(int maxElements) {
            return withResetValue(maxElements, 2 );
        }
        
        public static StableShape withResetValue(int maxElements, int resetValue) {
            if (resetValue < 2 || resetValue > 0xFF) {
                throw new IllegalArgumentException( "resetValue must be in the ranage [2,255]" );
            }
            Shape shape = Shape.fromNP(maxElements, 1.0/maxElements);
            double oneOverK = 1.0/shape.getNumberOfHashFunctions();
            double oneOverM = 1.0/shape.getNumberOfBits();
            double oneOverMax = 1.0/resetValue;
            double leftDenom = (1.0 / Math.pow(1-Math.pow(shape.getProbability(maxElements), oneOverK),oneOverMax))-1;
            double rightDenom = oneOverK - oneOverM;
            int numberofCellsDecremented = (int)Math.ceil(1.0/(leftDenom * rightDenom));

            return new StableShape( shape, resetValue, maxElements,numberofCellsDecremented);
        }
        
        private StableShape(Shape shape, int resetValue, int maxElements, int numberOfCellsDecremented) {
            this.shape = shape;
            this.resetValue = resetValue;
            this.maxElements = maxElements;
            this.numberOfCellsDecremented = numberOfCellsDecremented;
        }
        
        public Shape getShape() {
            return shape;
        }
        
        int getNumberOfHashFunctions() {
            return shape.getNumberOfHashFunctions();
        }
        
        int getNumberOfBits() {
            return shape.getNumberOfBits();
        }
        
        public double fps() {
            return shape.getProbability(maxElements);
        }
 
        
        /**
         * Definition 2 (Stable Point). The stable point is defined 
         * as the limit of the expected fraction of 0s in an SBF
         * when the number of iterations goes to infinity. When this
         * limit is reached, we call SBF stable
         */
        public double stablePoint() {
            double oneOverK = 1.0/shape.getNumberOfHashFunctions();
            double oneOverM = 1.0/shape.getNumberOfBits();
            return Math.pow(1.0/(1+(1.0/(numberOfCellsDecremented*(oneOverK-oneOverM)))),resetValue);
        }
        
        public int expectedCardinality() {
            return (int)Math.ceil((1.0-stablePoint()) * shape.getNumberOfBits());
        }
    }
    
    public StableBloomFilter(StableShape shape) {
        this.shape = shape;
        buffer = ByteBuffer.allocate(shape.getNumberOfBits());
        Random r = new Random();
        index = r.nextLong();
        increment = r.nextLong();
        count = 1;
        resetCount = this.shape.numberOfCellsDecremented;
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
        boolean[] duplicateFlag = {false};
        indexProducer.forEachIndex(x -> {
            duplicateFlag[0] |= (x == 0);
            return true;
        });
        return duplicateFlag[0];
    }

    @Override
    public int cardinality() {
        int result = 0;
        for (int i = 0; i < buffer.capacity(); i++) {
            if (buffer.get(i) > 0) {
                result++;
            }
        }
        return result;
    }

    @Override
    public boolean merge(final IndexProducer indexProducer) {
        decrement();
        return indexProducer.forEachIndex(x -> {
            if (x > shape.getNumberOfBits()) {
                return false;
            }
            buffer.put(x, (byte)shape.resetValue);
            return true;
        });
    }

    @Override
    public boolean forEachBitMap(LongPredicate consumer) {
        Objects.requireNonNull(consumer, "consumer");
        final int blocksm1 = BitMap.numberOfBitMaps(shape.getNumberOfBits()) - 1;
        int i = 0;
        long value;
        // must break final block separate as the number of bits may not fall on the
        // long boundary
        for (int j = 0; j < blocksm1; j++) {
            value = 0;
            for (int k = 0; k < Long.SIZE; k++) {
                if (buffer.get(i++) != 0) {
                    value |= BitMap.getLongBit(k);
                }
            }
            if (!consumer.test(value)) {
                return false;
            }
        }
        // Final block
        value = 0;
        for (int k = 0; i < shape.getNumberOfBits(); k++) {
            if (buffer.get(i++) != 0) {
                value |= BitMap.getLongBit(k);
            }
        }
        return consumer.test(value);

    }

    @Override
    public boolean forEachCount(BitCountConsumer consumer) {
        for (int i = 0; i < buffer.capacity(); i++) {
            byte b = buffer.get(i);
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

    private int asUnsignedInt(int position) {
        return 0xFF & buffer.get(position);
    }

    @Override
    public boolean add(BitCountProducer other) {
        return other.forEachCount((x, y) -> {
            if (x > shape.getNumberOfBits()) {
                return false;
            }
            long l = asUnsignedInt(x) + (long) y;
            buffer.put((byte) (l > 0xFF ? 0xFF : l));
            return true;
        });
    }

    @Override
    public boolean subtract(BitCountProducer other) {
        return other.forEachCount((x, y) -> {
            if (x > shape.getNumberOfBits()) {
                return false;
            }
            int l = asUnsignedInt(x) - y;
            buffer.put((byte) (l < 0 ? 0 : l));
            return true;
        });
    }

    @Override
    public CountingBloomFilter copy() {
        StableBloomFilter result = new StableBloomFilter(this.shape);
        forEachCount((x, y) -> {
            result.buffer.put(x, (byte) y);
            return true;
        });
        return result;
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

    public void decrement() {
        for (int i = 0; i < resetCount; i++) {
            tick();
        }
    }

    public void tick() {
        int result = mod(index, shape.getNumberOfBits());
        int b = asUnsignedInt(result);
        if (b > 0) {
            buffer.put(result, (byte) --b);
        }
        index -= increment;

        // Incorporate the counter into the increment to create a
        // tetrahedral number additional term.
        increment -= count++;
    }
}

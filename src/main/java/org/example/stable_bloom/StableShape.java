package org.example.stable_bloom;

import org.apache.commons.collections4.bloomfilter.Shape;

public class StableShape {
    
    private Shape shape;
    public final int resetValue;
    public final int maxElements;
    public final int numberOfCellsDecremented;
    public final byte bitsPerEntry;
    public final byte entriesPerByte;
    

    public static StableShape byMaxElements(int maxElements) {
        return withResetValue(maxElements, 2);
    }

    public static StableShape withResetValue(int maxElements, int resetValue) {
        if (resetValue < 2 || resetValue > 0xFF) {
            throw new IllegalArgumentException("resetValue must be in the ranage [2,255]");
        }
        Shape shape = Shape.fromNP(maxElements, 1.0 / maxElements);
        double oneOverK = 1.0 / shape.getNumberOfHashFunctions();
        double oneOverM = 1.0 / shape.getNumberOfBits();
        double oneOverMax = 1.0 / resetValue;
        double leftDenom = (1.0 / Math.pow(1 - Math.pow(shape.getProbability(maxElements), oneOverK), oneOverMax))
                - 1;
        double rightDenom = oneOverK - oneOverM;
        int numberofCellsDecremented = (int) Math.ceil(1.0 / (leftDenom * rightDenom));

        return new StableShape(shape, resetValue, maxElements, numberofCellsDecremented);
    }

    private StableShape(Shape shape, int resetValue, int maxElements, int numberOfCellsDecremented) {
        this.shape = shape;
        this.resetValue = resetValue;
        this.maxElements = maxElements;
        this.numberOfCellsDecremented = numberOfCellsDecremented;
        
        int bits = Byte.SIZE;
        for (int i=1;i<Byte.SIZE;i++) {
            if ((resetValue >> i)==0)
            {
                bits = i;
                break;
            }
        }
        this.bitsPerEntry = (byte) bits;
        this.entriesPerByte = (byte) (Byte.SIZE / bitsPerEntry);
    }

    public Shape getShape() {
        return shape;
    }

    int getNumberOfHashFunctions() {
        return shape.getNumberOfHashFunctions();
    }

    int getNumberOfEntries() {
        return shape.getNumberOfBits();
    }

    public double fps() {
        return shape.getProbability(maxElements);
    }

    /**
     * Definition 2 (Stable Point). The stable point is defined as the limit of the
     * expected fraction of 0s in an SBF when the number of iterations goes to
     * infinity. When this limit is reached, we call SBF stable
     */
    public double stablePoint() {
        double oneOverK = 1.0 / shape.getNumberOfHashFunctions();
        double oneOverM = 1.0 / shape.getNumberOfBits();
        return Math.pow(1.0 / (1 + (1.0 / (numberOfCellsDecremented * (oneOverK - oneOverM)))), resetValue);
    }

    public int expectedCardinality() {
        return (int) Math.ceil((1.0 - stablePoint()) * shape.getNumberOfBits());
    }

}
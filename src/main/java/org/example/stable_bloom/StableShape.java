package org.example.stable_bloom;

import org.apache.commons.collections4.bloomfilter.Shape;

public class StableShape {

    private Shape shape;
    /**
     * The value to set the cell when it is enabled.  In the paper this is called "Max".
     * resetValue = (2^bitsPerCell)-1
     */
    public final int resetValue;
    public final Shape decrementShape;
    public final int expectedCardinality;
    public final double fps;
    /**
     * Definition 2 (Stable Point). The stable point is defined as the limit of the
     * expected fraction of 0s in an SBF when the number of iterations goes to
     * infinity. When this limit is reached, we call SBF stable
     */
    public final double stablePoint;
    public final byte bitsPerCell;
    public final byte cellsPerByte;

    public static StableShape byShape(Shape shape) {
        return withBitsPerCell(shape, 2);
    }

    private static double fpFromK( int k ) {
        double p = 1 / Math.pow( Math.E, k);
        if (p==0.0) {
            throw new IllegalArgumentException("Probability must be greater than 0. p=" + p
                    + " Reduce number of hashes in shape: " + k);
        }
        return p;
    }

    public static StableShape withResetValue(Shape example_shape, int resetValue) {
        if (resetValue < 2 || resetValue > 0xFF) {
            throw new IllegalArgumentException("resetValue must be in the ranage [2,255]");
        }
        
        double fps = fpFromK( example_shape.getNumberOfHashFunctions() );
        Shape shape = Shape.fromPMK(fps, example_shape.getNumberOfBits(), example_shape.getNumberOfHashFunctions());
        double oneOverK = 1.0 / shape.getNumberOfHashFunctions();
        double oneOverM = 1.0 / shape.getNumberOfBits();
        double oneOverMax = 1.0 / resetValue;
        double leftDenom = 1.0 / Math.pow(1 - Math.pow(fps, oneOverK), oneOverMax) - 1;
        double rightDenom = oneOverK - oneOverM;
        int numberofCellsDecremented = (int) Math.ceil(1.0 / (leftDenom * rightDenom));

        double stablePoint = Math.pow(1.0 / (1 + (1.0 / (numberofCellsDecremented * (oneOverK - oneOverM)))),
                resetValue);
        return new StableShape(shape, resetValue, numberofCellsDecremented, stablePoint, fps);
    }
    
    public static StableShape withBitsPerCell(Shape shape, int bitsPerCell) {
        if (bitsPerCell<1 || bitsPerCell>Byte.SIZE) {
            throw new IllegalArgumentException("bitsPerCell must be in the ranage [1,8]");
        }
        int resetValue =  (1 << bitsPerCell)-1;
        return withResetValue(shape, resetValue);
    }

    private StableShape(Shape shape, int resetValue, int numberOfCellsDecremented,
            double stablePoint, double fps) {
        this.shape = Shape.fromPMK(fps, shape.getNumberOfBits(), shape.getNumberOfHashFunctions());
        this.resetValue = resetValue;
        this.decrementShape = Shape.fromKM(numberOfCellsDecremented, shape.getNumberOfBits());
        this.stablePoint = stablePoint;
        this.fps = fps;
        this.expectedCardinality =  (int) Math.ceil((1.0 - stablePoint) * shape.getNumberOfBits());
        
        int bits = Byte.SIZE;
        for (int i = 1; i < Byte.SIZE; i++) {
            if ((resetValue >> i) == 0) {
                bits = i;
                break;
            }
        }
        this.bitsPerCell = (byte) bits;
        this.cellsPerByte = (byte) (Byte.SIZE / bitsPerCell);
    }

    @Override
    public String toString() {
        return String.format(
                "StableShape[k=%s m=%s fps=%s stable point=%s expected cardinality=%s decrement count=%s reset value=%s]",
                getNumberOfHashFunctions(), getNumberOfEntries(), fps, stablePoint, expectedCardinality, 
                decrementShape.getNumberOfHashFunctions(), resetValue);
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
}
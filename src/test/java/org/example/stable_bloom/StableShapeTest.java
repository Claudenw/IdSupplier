package org.example.stable_bloom;

import org.apache.commons.collections4.bloomfilter.Shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

public class StableShapeTest {
    
    StableShape underTest;
    Shape testShape = Shape.fromNP(5, 1.0/5);

    @Test
    public void numberOfCellsDecrementedTest() {
        underTest = StableShape.byShape(testShape);
        assertEquals(14, underTest.decrementShape.getNumberOfHashFunctions());
    }
    
    @Test
    public void resetValueTest() {
        underTest = StableShape.byShape(testShape);
        assertEquals(3, underTest.resetValue);
        underTest = StableShape.withResetValue(testShape, 128);
        assertEquals(128, underTest.resetValue );
        
        underTest = StableShape.withResetValue(testShape, 250);
        assertEquals(250, underTest.resetValue );
        
        assertThrows( IllegalArgumentException.class, () -> StableShape.withResetValue(testShape, 300));
        assertThrows( IllegalArgumentException.class, () -> StableShape.withResetValue(testShape, 1));
    }
    
    @Test
    public void bitsPerEntryTest() {
        assertEquals( 2, StableShape.byShape(testShape).bitsPerCell);
        assertEquals( 2, StableShape.withResetValue(testShape,3).bitsPerCell);
        assertEquals( 3, StableShape.withResetValue(testShape,4).bitsPerCell);
        assertEquals( 3, StableShape.withResetValue(testShape,5).bitsPerCell);
        assertEquals( 3, StableShape.withResetValue(testShape,6).bitsPerCell);
        assertEquals( 3, StableShape.withResetValue(testShape,7).bitsPerCell);
        assertEquals( 4, StableShape.withResetValue(testShape,8).bitsPerCell);
        assertEquals( 8, StableShape.withResetValue(testShape,129).bitsPerCell);
        assertEquals( 8, StableShape.withResetValue(testShape,255).bitsPerCell);
    }
    
    @Test
    public void entriesPerByteTest() {
        assertEquals( 4, StableShape.byShape(testShape).cellsPerByte);
        assertEquals( 4, StableShape.withResetValue(testShape,3).cellsPerByte);
        assertEquals( 2, StableShape.withResetValue(testShape,4).cellsPerByte);
        assertEquals( 2, StableShape.withResetValue(testShape,5).cellsPerByte);
        assertEquals( 2, StableShape.withResetValue(testShape,6).cellsPerByte);
        assertEquals( 2, StableShape.withResetValue(testShape,7).cellsPerByte);
        assertEquals( 2, StableShape.withResetValue(testShape,8).cellsPerByte);
        assertEquals( 1, StableShape.withResetValue(testShape,16).cellsPerByte);
        assertEquals( 1, StableShape.withResetValue(testShape,129).cellsPerByte);
        assertEquals( 1, StableShape.withResetValue(testShape,255).cellsPerByte);
    }
    
    @Test
    public void expectedCardinalityTest() {
        assertEquals(7, StableShape.byShape(testShape).expectedCardinality);
    }
    
    @Test
    public void fpsTest() {
        StableShape shape = StableShape.byShape(testShape);
        double d = shape.getShape().estimateN(shape.expectedCardinality);
        int n = (int) Math.floor(d);
        assertEquals( shape.getShape().getProbability(n), shape.fps, 0.006);
    }
    
    @Test
    public void getNumberOfEntriesTest() {
        assertEquals( 17, StableShape.byShape(testShape).getNumberOfEntries());
    }
    
    @Test
    public void getNumberOfHashFunctionsTest() {
        assertEquals( 2, StableShape.byShape(testShape).getNumberOfHashFunctions());
    }
    
    @Test
    public void getShapeTest() {
        Shape shape = StableShape.byShape(testShape).getShape();
        assertNotNull(shape);
        assertEquals( 17, shape.getNumberOfBits());
        assertEquals( 2, shape.getNumberOfHashFunctions());
        assertEquals( 1.0/5, shape.getProbability( 5 ), 0.01);
    }
    
    @Test
    public void stablePointTest() {
        assertEquals( 0.638, StableShape.byShape(testShape).stablePoint, 0.001);
    }
}

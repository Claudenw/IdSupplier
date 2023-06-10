package org.example.stable_bloom;

import org.apache.commons.collections4.bloomfilter.Shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

public class StableShapeTest {
    
    StableShape underTest;

    @Test
    public void maxElementsTest() {
        underTest = StableShape.byMaxElements(5);
        assertEquals(5, underTest.maxElements );
        underTest = StableShape.withResetValue(6, 128);
        assertEquals(6, underTest.maxElements );
        assertThrows( IllegalArgumentException.class, () -> StableShape.withResetValue(6,  257 ) );
    }

    @Test
    public void numberOfCellsDecrementedTest() {
        underTest = StableShape.byMaxElements(5);
        assertEquals(7, underTest.numberOfCellsDecremented );
    }
    
    @Test
    public void resetValueTest() {
        underTest = StableShape.byMaxElements(5);
        assertEquals(2, underTest.resetValue);
        underTest = StableShape.withResetValue(6, 128);
        assertEquals(128, underTest.resetValue );
        
        underTest = StableShape.withResetValue(6, 250);
        assertEquals(250, underTest.resetValue );
        
        assertThrows( IllegalArgumentException.class, () -> StableShape.withResetValue(6, 300));
        assertThrows( IllegalArgumentException.class, () -> StableShape.withResetValue(6, 1));
    }
    
    @Test
    public void bitsPerEntryTest() {
        assertEquals( 2, StableShape.byMaxElements(5).bitsPerEntry());
        assertEquals( 2, StableShape.withResetValue(5,3).bitsPerEntry());
        assertEquals( 3, StableShape.withResetValue(5,4).bitsPerEntry());
        assertEquals( 3, StableShape.withResetValue(5,5).bitsPerEntry());
        assertEquals( 3, StableShape.withResetValue(5,6).bitsPerEntry());
        assertEquals( 3, StableShape.withResetValue(5,7).bitsPerEntry());
        assertEquals( 4, StableShape.withResetValue(5,8).bitsPerEntry());
        assertEquals( 8, StableShape.withResetValue(5,129).bitsPerEntry());
        assertEquals( 8, StableShape.withResetValue(5,255).bitsPerEntry());
    }
    
    @Test
    public void expectedCardinalityTest() {
        assertEquals( 8, StableShape.byMaxElements(5).expectedCardinality());
    }
    
    @Test
    public void fpsTest() {
        assertEquals( 1.0/5, StableShape.byMaxElements(5).fps(), 0.01);
    }
    
    @Test
    public void getNumberOfEntriesTest() {
        assertEquals( 17, StableShape.byMaxElements(5).getNumberOfEntries());
    }
    
    @Test
    public void getNumberOfHashFunctionsTest() {
        assertEquals( 2, StableShape.byMaxElements(5).getNumberOfHashFunctions());
    }
    
    @Test
    public void getShapeTest() {
        Shape shape = StableShape.byMaxElements(5).getShape();
        assertNotNull(shape);
        assertEquals( 17, shape.getNumberOfBits());
        assertEquals( 2, shape.getNumberOfHashFunctions());
        assertEquals( 1.0/5, shape.getProbability( 5 ), 0.01);
    }
    
    @Test
    public void stablePointTest() {
        assertEquals( 0.57, StableShape.byMaxElements(5).stablePoint(), 0.001);
    }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.example.stable_bloom;

import org.apache.commons.collections4.bloomfilter.AbstractBitMapProducerTest;
import org.apache.commons.collections4.bloomfilter.BitMapProducer;
import org.apache.commons.collections4.bloomfilter.Shape;
import org.apache.commons.collections4.bloomfilter.Hasher;
import org.apache.commons.collections4.bloomfilter.BloomFilter;
import org.apache.commons.collections4.bloomfilter.IncrementingHasher;

public class BitMapProducerFromStableBloomFilterTest extends AbstractBitMapProducerTest {

    StableShape stableShape = StableShape.byShape(Shape.fromPMK(0.05, 72, 17));

    @Override
    protected BitMapProducer createProducer() {
        final Hasher hasher = new IncrementingHasher(0, 1);
        final BloomFilter bf = new StableBloomFilter(stableShape);
        bf.merge(hasher);
        return bf;
    }

    @Override
    protected BitMapProducer createEmptyProducer() {
        return new StableBloomFilter(stableShape);
    }
}
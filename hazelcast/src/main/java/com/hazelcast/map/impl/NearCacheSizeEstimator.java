/*
 * Copyright (c) 2008-2015, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.map.impl;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import static com.hazelcast.util.JVMUtil.OBJECT_HEADER_IN_BYTES;

/**
 * Size estimator for near cache.
 */
class NearCacheSizeEstimator implements SizeEstimator<NearCacheRecord> {

    private static final AtomicLongFieldUpdater<NearCacheSizeEstimator> SIZE_UPDATER = AtomicLongFieldUpdater
            .newUpdater(NearCacheSizeEstimator.class, "size");

    private volatile long size;

    protected NearCacheSizeEstimator() {
        super();
    }

    @Override
    public long getCost(NearCacheRecord record) {
        // immediate check nothing to do if record is null
        if (record == null) {
            return 0;
        }
        final long cost = record.getCost();
        // if  cost is zero, type of cached object is not Data.
        // then omit.
        if (cost == 0) {
            return 0;
        }
        // entry size in CHM
        final int numberOfIntegers = 4;
        return numberOfIntegers * (Integer.SIZE / Byte.SIZE) + OBJECT_HEADER_IN_BYTES + cost;
    }

    @Override
    public long getSize() {
        return size;
    }

    public void add(long size) {
        SIZE_UPDATER.addAndGet(this, size);
    }

    public void reset() {
        SIZE_UPDATER.set(this, 0L);
    }
}

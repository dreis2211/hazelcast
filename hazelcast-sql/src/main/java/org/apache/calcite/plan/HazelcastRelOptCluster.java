/*
 * Copyright (c) 2008-2020, Hazelcast, Inc. All Rights Reserved.
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

package org.apache.calcite.plan;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rex.RexBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Extended RelOptCluster with Hazelcast-specific data.
 */
public final class HazelcastRelOptCluster extends RelOptCluster {
    /** Current cluster. */
    private static final ThreadLocal<HazelcastRelOptCluster> CURRENT = new ThreadLocal<>();

    /** Number of members. */
    private final int memberCount;

    private HazelcastRelOptCluster(
        RelOptPlanner planner,
        RelDataTypeFactory typeFactory,
        RexBuilder rexBuilder,
        AtomicInteger nextCorrel,
        Map<String, RelNode> mapCorrelToRel,
        int memberCount
    ) {
        super(planner, typeFactory, rexBuilder, nextCorrel, mapCorrelToRel);

        this.memberCount = memberCount;
    }

    public static HazelcastRelOptCluster create(RelOptPlanner planner, RexBuilder rexBuilder, int memberCount) {
        return new HazelcastRelOptCluster(
            planner,
            rexBuilder.getTypeFactory(),
            rexBuilder,
            new AtomicInteger(0),
            new HashMap<>(),
            memberCount
        );
    }

    public static HazelcastRelOptCluster tryCast(RelOptCluster cluster) {
        if (cluster instanceof HazelcastRelOptCluster) {
            return cast(cluster);
        } else {
            return null;
        }
    }

    public static HazelcastRelOptCluster cast(RelOptCluster cluster) {
        assert cluster instanceof HazelcastRelOptCluster;

        return (HazelcastRelOptCluster) cluster;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void startPhysicalOptimization() {
        assert CURRENT.get() == null;

        CURRENT.set(this);
    }

    public void finishPhysicalOptimization() {
        assert CURRENT.get() == this;

        CURRENT.set(null);
    }

    public static boolean isSingleMember() {
        HazelcastRelOptCluster current = CURRENT.get();

        return current != null && current.getMemberCount() == 1;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{memberCount=" + memberCount + '}';
    }
}
/*
 * Copyright (c) 2008-2016, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.jet.benchmark;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.core.Partition;
import com.hazelcast.internal.util.ThreadLocalRandom;
import com.hazelcast.jet.DAG;
import com.hazelcast.jet.Edge;
import com.hazelcast.jet.JetEngine;
import com.hazelcast.jet.JetEngineConfig;
import com.hazelcast.jet.ProcessorMetaSupplier;
import com.hazelcast.jet.ProcessorSupplier;
import com.hazelcast.jet.TestProcessors;
import com.hazelcast.jet.Vertex;
import com.hazelcast.jet.impl.AbstractProducer;
import com.hazelcast.jet.impl.IMapWriter;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.TestHazelcastInstanceFactory;
import com.hazelcast.test.annotation.NightlyTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;

import static com.hazelcast.jet.impl.Util.uncheckedGet;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Category(NightlyTest.class)
@RunWith(HazelcastSerialClassRunner.class)
public class BackpressureTest extends HazelcastTestSupport {

    private static final int CLUSTER_SIZE = 2;
    private static final int TOTAL_PARALLELISM = Runtime.getRuntime().availableProcessors();
    private static final int PARALLELISM_PER_MEMBER = TOTAL_PARALLELISM / CLUSTER_SIZE;
    private static final int DISTINCT = 1000;
    private static final int COUNT_PER_DISTINCT_AND_SLICE = 10_000;

    private TestHazelcastInstanceFactory factory;
    private HazelcastInstance hz1;
    private HazelcastInstance hz2;
    private JetEngine jetEngine;

    @Before
    public void setUp() {
        factory = createHazelcastInstanceFactory();
        hz1 = factory.newHazelcastInstance();
        hz2 = factory.newHazelcastInstance();
        warmUpPartitions(asList(hz1, hz2));
        assertEquals(2, hz1.getCluster().getMembers().size());
        JetEngineConfig config = new JetEngineConfig().setParallelism(PARALLELISM_PER_MEMBER);
        jetEngine = JetEngine.get(hz1, "jetEngine", config);
    }

    @After
    public  void afterClass() {
        factory.shutdownAll();
    }

    @Test
    public void testBackpressure() throws Exception {
        DAG dag = new DAG();

        final int member1Port = hz1.getCluster().getLocalMember().getAddress().getPort();
        final Member member2 = hz2.getCluster().getLocalMember();
        final int ptionOwnedByMember2 =
                hz1.getPartitionService()
                   .getPartitions().stream()
                   .filter(p -> p.getOwner().equals(member2))
                   .map(Partition::getPartitionId)
                   .findAny()
                   .orElseThrow(() -> new RuntimeException("Can't find a partition owned by member " + hz2));
        Vertex generator = new Vertex("generator", (ProcessorMetaSupplier) address -> address.getPort() == member1Port
                ? ProcessorSupplier.of(GeneratingProducer::new)
                : ProcessorSupplier.of(TestProcessors.NoopProducer::new)
        );
        Vertex hiccuper = new Vertex("hiccuper", ProcessorMetaSupplier.of(Hiccuper::new));
        Vertex consumer = new Vertex("consumer", IMapWriter.supplier("counts"));
        dag
                .addVertex(generator)
                .addVertex(hiccuper)
                .addVertex(consumer)
                .addEdge(new Edge(generator, hiccuper)
                        .distributed()
                        .partitionedByCustom((x, y) -> ptionOwnedByMember2))
                .addEdge(new Edge(hiccuper, consumer));

        uncheckedGet(jetEngine.newJob(dag).execute());
        assertCounts(hz1.getMap("counts"));
    }

    private static void assertCounts(Map<String, Long> wordCounts) {
        for (int i = 0; i < DISTINCT; i++) {
            Long count = wordCounts.get(Integer.toString(i));
            assertNotNull("Missing count for " + i, count);
            assertEquals("The count for " + i + " is not correct",
                    COUNT_PER_DISTINCT_AND_SLICE * PARALLELISM_PER_MEMBER, (long) count);
        }
    }

    private static class GeneratingProducer extends AbstractProducer {

        private int item;
        private int count;

        @Override
        public boolean complete() {
            while (!getOutbox().isHighWater()) {
                emit(new SimpleImmutableEntry<>(Integer.toString(item), 1L));
                item++;
                if (item == DISTINCT) {
                    if (++count == COUNT_PER_DISTINCT_AND_SLICE) {
                        return true;
                    }
                    item = 0;
                }
            }
            return false;
        }
    }

    private static class Hiccuper extends WordCountTest.Combiner {

        private long hiccupDeadline;
        private long nextHiccupTime;

        Hiccuper() {
            updateNextHiccupTime();
        }

        @Override
        public boolean process(int ordinal, Object item) {
            if (isHiccuping()) {
                return false;
            }
            return super.process(ordinal, item);
        }

        private boolean isHiccuping() {
            if (hiccupDeadline != 0) {
                if (System.nanoTime() < hiccupDeadline) {
                    return true;
                }
                System.out.println("==== Resume");
                hiccupDeadline = 0;
                updateNextHiccupTime();
            }
            if (System.nanoTime() >= nextHiccupTime) {
                final long hiccupDuration = MILLISECONDS.toNanos(301)
                        + ThreadLocalRandom.current().nextLong(MILLISECONDS.toNanos(570));
                hiccupDeadline = System.nanoTime() + hiccupDuration;
                System.out.println("==== Hiccup " + NANOSECONDS.toMillis(hiccupDuration) + " ms");
            }
            return false;
        }

        private void updateNextHiccupTime() {
            nextHiccupTime = System.nanoTime() + MILLISECONDS.toNanos(700)
                    + MILLISECONDS.toNanos(ThreadLocalRandom.current().nextLong(2_000));
        }
    }
}

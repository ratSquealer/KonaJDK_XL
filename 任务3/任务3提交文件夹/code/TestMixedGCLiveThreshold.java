/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package gc.g1;

///*
// * @test id=0percent
// * @summary Test G1MixedGCLiveThresholdPercent=0. Fill up a region to at least 33 percent,
// * the region should not be selected for mixed GC cycle.
// * @requires vm.gc.G1
// * @library /test/lib
// * @build jdk.test.whitebox.WhiteBox
// * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
// * @run driver gc.g1.TestMixedGCLiveThreshold 0 false
// */
//
///*
// * @test id=25percent
// * @summary Test G1MixedGCLiveThresholdPercent=25. Fill up a region to at least 33 percent,
// * the region should not be selected for mixed GC cycle.
// * @requires vm.gc.G1
// * @library /test/lib
// * @build jdk.test.whitebox.WhiteBox
// * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
// * @run driver gc.g1.TestMixedGCLiveThreshold 25 false
// */
//
/*
 * @test id=80percent
 * @summary Test G1MixedGCLiveThresholdPercent=80. Fill up a region to at least 33 percent,
 * the region should be selected for mixed GC cycle.
 * @requires vm.gc.G1
 * @library /test/lib
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/timeout=300 gc.g1.TestMixedGCLiveThreshold 80 true
 */
// * @run driver gc.g1.TestMixedGCLiveThreshold 80 true
///*
// * @test id=85percent
// * @summary Test G1MixedGCLiveThresholdPercent=85. Fill up a region to at least 33 percent,
// * the region should be selected for mixed GC cycle.
// * @requires vm.gc.G1
// * @library /test/lib
// * @build jdk.test.whitebox.WhiteBox
// * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
// * @run driver gc.g1.TestMixedGCLiveThreshold 85 true
// */

///*
// * @test id=90percent
// * @summary Test G1MixedGCLiveThresholdPercent=90. Fill up a region to at least 33 percent,
// * the region should be selected for mixed GC cycle.
// * @requires vm.gc.G1
// * @library /test/lib
// * @build jdk.test.whitebox.WhiteBox
// * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
// * @run driver gc.g1.TestMixedGCLiveThreshold 90 true
// */

///*
// * @test id=95percent
// * @summary Test G1MixedGCLiveThresholdPercent=95. Fill up a region to at least 33 percent,
// * the region should be selected for mixed GC cycle.
// * @requires vm.gc.G1
// * @library /test/lib
// * @build jdk.test.whitebox.WhiteBox
// * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
// * @run driver gc.g1.TestMixedGCLiveThreshold 95 true
// */

///*
// * @test id=100percent
// * @summary Test G1MixedGCLiveThresholdPercent=100. Fill up a region to at least 33 percent,
// * the region should be selected for mixed GC cycle.
// * @requires vm.gc.G1
// * @library /test/lib
// * @build jdk.test.whitebox.WhiteBox
// * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
// * @run driver gc.g1.TestMixedGCLiveThreshold 100 true
// */



import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.security.SecureRandom;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Asserts;
import jdk.test.whitebox.WhiteBox;
import java.util.List;
import java.util.ArrayList;
public class TestMixedGCLiveThreshold {
    private static final String pattern = "Remembered Set Tracking update regions total ([0-9]+), selected ([0-9]+)$";

    public static void main(String[] args) throws Exception {
        System.setProperty("test.timeout.factor", "100"); // 设置超时时间的乘数为60
        int liveThresholdPercent = Integer.parseInt(args[0]);
        boolean expectRebuild = Boolean.parseBoolean(args[1]);
//        testMixedGCLiveThresholdPercent(liveThresholdPercent, expectRebuild);
        OutputAnalyzer output = testWithMixedGCLiveThresholdPercent(liveThresholdPercent);
        output.reportDiagnosticSummary();
    }


    private static OutputAnalyzer testWithMixedGCLiveThresholdPercent(int percent) throws Exception {
        ArrayList<String> basicOpts = new ArrayList<>();
        String gcLogFile = String.format("/home/cxl/projects/TencentKona/TencentKona-17-TencentKona-17.0.11/G1GC_%d_1.log", percent);
//        String gcLogFileRemset = String.format("/home/cxl/projects/TencentKona/TencentKona-17-TencentKona-17.0.11/G1GCRemset_%d.log", percent);
        String gcLogFileIHOP = String.format("/home/cxl/projects/TencentKona/TencentKona-17-TencentKona-17.0.11/G1GCIHOP_%d_1.log", percent);
        String gcLogFileCset = String.format("/home/cxl/projects/TencentKona/TencentKona-17-TencentKona-17.0.11/G1GCCset_%d_1.log", percent);
        Collections.addAll(basicOpts, new String[] {
                                       "-Xbootclasspath/a:.",
                                       "-XX:+UseG1GC",
                                       "-XX:+UnlockDiagnosticVMOptions",
                                       "-XX:+UnlockExperimentalVMOptions",
//                                       "-XX:G1NewSizePercent=5",        // ---------
//                                       "-XX:G1MaxNewSizePercent=10",    // ---------
                                       "-XX:+WhiteBoxAPI",
                                       "-XX:G1HeapRegionSize=2m",
                                       "-XX:MaxGCPauseMillis=50",
//                                       "-XX:InitiatingHeapOccupancyPercent=5",
//                                       "-XX:G1HeapWastePercent=0",
//                                       "-XX:G1ReservePercent=0",
//                                       "-Xlog:gc+remset+tracking=trace:" + gcLogFileRemset + ":time",
                                       "-Xlog:gc+ihop=trace:" + gcLogFileIHOP + ":time",
                                       "-Xlog:gc*:file=" + gcLogFile + ":time,level",
                                       "-Xlog:gc+ergo+cset=debug:"+ gcLogFileCset + ":time,level,tags",
                                       "-Xms128m",//修改这里！！！
                                       "-Xmx128m"});//修改这里！！！

        basicOpts.add("-XX:G1MixedGCLiveThresholdPercent=" + percent);

        basicOpts.add(GCTest.class.getName());
        basicOpts.add(String.valueOf(percent));
        ProcessBuilder procBuilder =  ProcessTools.createJavaProcessBuilder(basicOpts);
        OutputAnalyzer analyzer = new OutputAnalyzer(procBuilder.start());
        return analyzer;
    }

    private static boolean regionsSelectedForRebuild(String output) throws Exception {

        Matcher m = Pattern.compile(pattern, Pattern.MULTILINE).matcher(output);
        if (!m.find()) {
            throw new Exception("Could not find correct output for Remembered Set Tracking in stdout," +
              " should match the pattern \"" + pattern + "\", but stdout is \n" + output);
        }
        return Integer.parseInt(m.group(2)) > 0;
    }

    public static class GCTest {
        public static void main(String args[]) throws Exception {
            WhiteBox wb = WhiteBox.getWhiteBox();
            int liveThresholdPercent = Integer.parseInt(args[0]);
            System.out.println("--------------------------------------------------------------------------");
            System.out.println("now liveThresholdPercent" + liveThresholdPercent);
            // 获取老年代区域的数量和信息
            long maxRegions = wb.g1NumMaxRegions();
            int regionSize = wb.g1RegionSize();
            System.out.println("Total regions: " + maxRegions + ", Region size: " + regionSize);

            System.out.println("--------------------------------------------------------------------------");
            System.out.println("Before AllocateMemory:");
            printOldRegionInfo(wb, liveThresholdPercent);
            // ------------ 核心部分 ----------------
            LRUCache cache = new LRUCache(200);
            LRUCache Hcache = new LRUCache(3);
            allocateMemory(wb, 3000, liveThresholdPercent, cache, Hcache);
            // ------------ 核心部分 ----------------

            System.out.println(" ------------ after 3000iters old region ---------" );
            printOldRegionInfo(wb, liveThresholdPercent);
//
//            Thread.sleep(10);
//            System.out.println("----------------------- 主动触发 Start Concurrent Mark Cycle------------------------------");
//            wb.g1StartConcMarkCycle();
//            System.out.println("check In Concurrent Mark Cycle: " + wb.g1InConcurrentMark());
//            while (wb.g1InConcurrentMark()) {
//                Thread.sleep(1000);
//            }
//            System.out.println("check In Concurrent Mark Cycle after sleep 1000: " + wb.g1InConcurrentMark());
//            // 5. 并发标记周期结束后，打印老年代状态
//            System.out.println("After Concurrent Mark Cycle:");
//            printOldRegionInfo(wb, liveThresholdPercent);
//
//
//            // 3. 再分配一些对象到老年代，观察对象提升情况
//            allocateMemory(wb, 500, liveThresholdPercent,cache);  // 分配50个对象
//            System.out.println("------------------ After AllocateMemory10 and and Before Concurrent Mark Cycle:------------------------");
//            printOldRegionInfo(wb, liveThresholdPercent);
//
//            // 4. 启动并发标记周期，并确保它结束
//            wb.g1StartConcMarkCycle();
//            System.out.println("check In Concurrent Mark Cycle: " + wb.g1InConcurrentMark());
//            while (wb.g1InConcurrentMark()) {
//                Thread.sleep(1000);
//            }
//            System.out.println("check In Concurrent Mark Cycle after sleep 1000: " + wb.g1InConcurrentMark());
//            // 5. 并发标记周期结束后，打印老年代状态
//            System.out.println("------------ After 手动 Concurrent Mark Cycle:------------------");
//            printOldRegionInfo(wb, liveThresholdPercent);

        }

        private static void allocateMemory(WhiteBox wb, int count, int liveThresholdPercent, LRUCache cache, LRUCache Hcache) {
            final int objectSize_small = WhiteBox.getWhiteBox().g1RegionSize() / 9;
            final int objectSize = WhiteBox.getWhiteBox().g1RegionSize();

            for (int i = 0; i < count; i++) {
                SecureRandom randPut = new SecureRandom();
                SecureRandom randGet = new SecureRandom();
                cache.put(i, new byte[objectSize_small]);
                cache.put(randPut.nextInt(100), new byte[objectSize_small * 2]);
                cache.get(randGet.nextInt(100));
                //allocations.add(new byte[objectSize]);
                if(i % 100 == 0){
                    Hcache.put(randPut.nextInt(3), new byte[objectSize_small * 8]);
                    Hcache.get(randPut.nextInt(3));
                }
                if(i % 10 == 0){
                    System.out.println("----------- After allocat every objectSize/objectSize_small-----------------");
                    printOldRegionInfo(wb, liveThresholdPercent);
                }
                if(wb.g1InConcurrentMark()) {
                    System.out.println("----------- Before Pause Young (Concurrent Start)(G1 Preventive Collection) / (G1 Evacuation Pause)  -----------------");
                    printOldRegionInfo(wb, liveThresholdPercent);
                    while (wb.g1InConcurrentMark()) {
//                        System.out.println("----------- 大量分配小对象，晋升old region，导致自我调配 现在正在执行四个阶段中-----------------");
//                        System.out.println("----------- Pause Young (Concurrent Start) (G1 Preventive Collection) / (G1 Evacuation Pause)-----------------");
                        try {
                            Thread.sleep(50); // 睡眠1s
                        } catch (InterruptedException e) {
                            System.out.println("Thread was interrupted.");
                        }
                    }
                    System.out.println("----------- After Pause Young (Concurrent Start) (G1 Preventive Collection) / (G1 Evacuation Pause)  -----------------");
                    printOldRegionInfo(wb, liveThresholdPercent);
                }

                try {
                    Thread.sleep(20); // 睡眠20毫秒
                } catch (InterruptedException e) {
                    System.out.println("Thread was interrupted.");
                }
            }
//            LRUCache cache1 = new LRUCache(5);
//            for (int i = 0; i < 50; i++) {
//                SecureRandom randPut1 = new SecureRandom();
//                cache1.put(i, new byte[objectSize * 2]);
//                cache1.put(randPut1.nextInt(5), new byte[objectSize]);
////                cache.put(randPut1.nextInt(count), new byte[objectSize]);
//                //allocations.add(new byte[objectSize]);
//                if(i % 5 == 0){
//                    SecureRandom randGet1 = new SecureRandom();
//                    cache1.get(randGet1.nextInt(3));
//                }
//
//                if(wb.g1InConcurrentMark()) {
//                    System.out.println("----------- Before Pause Young (Concurrent Start) (G1 Humongous Allocation)  -----------------");
//                    printOldRegionInfo(wb, liveThresholdPercent);
//                    while (wb.g1InConcurrentMark()){
//                        try {
//                            Thread.sleep(1000); // 睡眠1s
//                        } catch (InterruptedException e) {
//                            System.out.println("Thread was interrupted.");
//                        }
//                    }
//                    System.out.println("----------- After Pause Young (Concurrent Start) (G1 Humongous Allocation)   -----------------");
//                    printOldRegionInfo(wb, liveThresholdPercent);
////                        System.out.println("----------- After Mixed (G1 Humongous Allocation) -----------------");
////                        printOldRegionInfo(wb, liveThresholdPercent);
//                }
//                try {
//                    Thread.sleep(500); // 睡眠20毫秒
//                } catch (InterruptedException e) {
//                    System.out.println("Thread was interrupted.");
//                }
//            }

        }


        // 打印老年代区域的信息
        private static void printOldRegionInfo(WhiteBox wb, int Percent) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS");
            LocalDateTime now = LocalDateTime.now();
            System.out.println("Current DateTime: " + dtf.format(now));


            long[] mixedGCInfo = wb.g1GetMixedGCInfo(Percent);
            int maxRegions = (int)wb.g1NumMaxRegions();
            int oldRegionNums = 0;
            for (int i = 0; i < maxRegions; i++) {
                try {
                    long[] regionInfo = wb.g1GetRegionInfo(i);
                    if (regionInfo != null) {
                        oldRegionNums = oldRegionNums + 1;
                        System.out.println("Region " + i + ":" + "Address: 0x" + Long.toHexString(regionInfo[3]) + "  Live bytes: " + regionInfo[0] + "  Total bytes: " + regionInfo[1] + "  Live percent: " + regionInfo[2] + "%");
//                        System.out.println("Region " + i + ":" + "Address: 0x" + "  Live bytes: " + regionInfo[0] + "  Total bytes: " + regionInfo[1] + "  Live percent: " + regionInfo[2] + "%");
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("Error fetching region info for region " + i + ": " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("Unexpected error when fetching region info: " + e.getMessage());
                }
            }
            System.out.println("Old Regions Count: " + oldRegionNums);
            System.out.println("match Old Regions Count: " + mixedGCInfo[0]);
            System.out.println("Total Memory in Old Regions: " + mixedGCInfo[1]);
            System.out.println("Estimated Memory to Free in Old Regions: " + mixedGCInfo[2]);

        }

    }


    public static class DLinkedNode {
        int key;
        byte[] value;
        DLinkedNode prev;
        DLinkedNode next;
        public DLinkedNode() {}
        public DLinkedNode(int _key, byte[] _value) {key = _key; value = _value;}
        public DLinkedNode(byte[] _value) {value = _value;}
    }
    public static class LRUCache {


        private HashMap<Integer, DLinkedNode> cache = new HashMap<>();
        private int size;
        private int capacity;
        private DLinkedNode head, tail;

        public LRUCache(int capacity) {
            this.size = 0;
            this.capacity = capacity;
            head = new DLinkedNode();
            tail = new DLinkedNode();
            head.next = tail;
            tail.prev = head;
        }

        public byte[] get(int key) {
            DLinkedNode node = cache.get(key);
            if (node == null) {
                return new byte[0];
            }
            moveToHead(node);
            return node.value;
        }

        public void put(int key, byte[] value) {
            DLinkedNode node = cache.get(key);
            if (node == null) {
                DLinkedNode newNode = new DLinkedNode(key, value);
                cache.put(key, newNode);
                addToHead(newNode);
                ++size;
                if (size > capacity) {
                    DLinkedNode tail = removeTail();
                    cache.remove(tail.key);
                    --size;
                }
            } else {
                node.value = value;
                moveToHead(node);
            }
        }

        private void addToHead(DLinkedNode node) {
            node.prev = head;
            node.next = head.next;
            head.next.prev = node;
            head.next = node;
        }

        private void removeNode(DLinkedNode node) {
            node.prev.next = node.next;
            node.next.prev = node.prev;
        }

        private void moveToHead(DLinkedNode node) {
            removeNode(node);
            addToHead(node);
        }

        private DLinkedNode removeTail() {
            DLinkedNode res = tail.prev;
            removeNode(res);
            return res;
        }
    }
}


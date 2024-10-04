
import java.text.DateFormat;			// 格式化和解析日期
import java.text.SimpleDateFormat;		// DateFormat 的一个具体子类，用于以区域设置敏感的方式格式化和解析日期
import java.util.Date;					// 提供关于特定瞬间的日期和时间信息，精确到毫秒
import java.util.Iterator;				// 用于遍历集合（如列表、集合）的元素
import java.util.LinkedHashMap;		// 基于哈希表的 Map 接口的实现，具有可预知的迭代顺序。
import java.util.LinkedList;				// 实现了List接口的链表结构，提供了列表操作和队列操作的方法。
import java.util.List;					//  是一个有序集合接口，可以精确控制列表中每个元素的插入位置
import java.util.Map.Entry;				// Map中的键值对。这个接口通常用于遍历Ma
import java.util.Random;				// 生成伪随机数
import java.util.concurrent.atomic.AtomicLong;	// 一个在多线程环境中可以不使用锁定且线程安全的变量类，用于表示一个长整型(long)

public class BigRamTester {
	// 这行定义了一个日期格式化对象，用来以 "年-月-日 时:分:秒" 的格式显示日期和时间
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
    	// 常量 RESERVED_SPACE，表示保留的内存空间大小为10GB。这可能用于程序中管理内存使用或预留内存的部分
    private static final long RESERVED_SPACE = 10L * 1024L * 1024L * 1024L; // 10 GB
	// 用来限制列表的最大长度，这里设为30。这个大小可能用于限制内存中存储数据结构的元素数量
    private static final int MAX_LIST_SIZE = 30;
	// 表示每条记录大约占用1000字节的内存。这可能用于估计内存使用量或者配置缓存策略。
    private static final int APPROX_BYTES_PER_RECORD = 1000;
	// 定义了目标命中率为90%，可能用于衡量缓存效果或其他性能指标
    private static final int TARGET_HIT_RATE = 90;
	// Random 对象用于生成随机数
    private static final Random RANDOM = new Random();
	// 跟踪程序开始执行的时间
    private static long startTime;
	// LinkedHashMap 保持了元素的插入顺序，这可能用于确保遍历顺序或其他有序操作。
    private LinkedHashMap<Integer, List<Integer>> storage;
	// totalSum，用来累计一些值
    private long totalSum = 0;
	// Statistics 类的对象，用于跟踪和管理统计数据
    private final Statistics stats = new Statistics();

    public static void main(String[] args) {
        startTime = System.currentTimeMillis();	// 记录程序开始运行的时间
        try {
            new BigRamTester().go();			// 创建 BigRamTester 类的实例并调用其 go 方法
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void go() throws InterruptedException {
        int keys = createData();
        accessCache(keys);
        System.out.println(totalSum);
    }
    // 用于模拟访问和更新一个缓存（在这个例子中是 storage，一个 LinkedHashMap）
    private void accessCache(int keys) throws InterruptedException {
        // 用于随机选择键值的范围。这是通过将键的数量调整为目标命中率的百分比来实现，以此模拟对缓存中现有条目的重复访问和对缺失条目的访问。
        int dataRange = ((keys - 1) / TARGET_HIT_RATE) * 100;
        // 无限循环，用于不断地访问和更新缓存。
        while (true) {
            // 缓存操作开始的时间，用于计算操作的耗时
            long startTime = System.nanoTime();
            // 随机生成一个键值，用于访问 storage 中的数据。这个键值是在 [0, dataRange] 区间内
            int key = RANDOM.nextInt(dataRange + 1);
            // 尝试从 storage 中获取键对应的数据
            List<Integer> data = storage.get(key);

            // 如果获取的数据为 null，即缓存未命中，则创建一个新的数据列表，将其与键一起放入 storage 中。
            // 之后，移除缓存中的第一个条目，以模拟 LinkedHashMap 的 LRU 行为。同时，记录一个缓存未命中的事件。
            // 如果获取的数据不为 null，即缓存命中，则记录一个缓存命中的事件。
            if (data == null) {
                data = createIntegers();
                storage.put(key, data);
                Iterator<Entry<Integer, List<Integer>>> iterator = storage.entrySet().iterator();
                iterator.next();
                iterator.remove();
                stats.registerMiss();
            } else {
                stats.registerHit();
            }

            // 记录操作结束的时间并计算整个操作的耗时，将结果记录到统计对象 stats 中
            long endTime = System.nanoTime();
            stats.recordTimeTaken(endTime - startTime);            
            // 计算获取的数据列表中所有整数的和
            int sum = 0;
            for (Integer dataItem : data) {
                sum += dataItem;
            }

            totalSum += sum;

        }

    }

    private int createData() {

        // Try and estimate the number of records that we will store to avoid resizing the hashmap many times.
        // This is based on the fixed APPROX_BYTES_PER_RECORD value, which is based on observing the amount of memory used
        // with a fixed value of MAX_LIST_SIZE
        //尝试估计我们将存储的记录数量，以避免多次调整哈希映射的大小。
        // 这是基于固定的APPROX_BYTES_PER_RECORD值，该值基于观察所使用的内存量
        // 具有固定值MAX_LIST_SIZE
        // 这一行计算了大约可以存储的记录数。计算结果加1000是为了保证即使估算稍有不准也有足够的初始容量，避免频繁调整 HashMap 的大小
        int estimatedNumberOfRecords = (int) ((Runtime.getRuntime().freeMemory() - RESERVED_SPACE) / APPROX_BYTES_PER_RECORD) + 1000;
        // 打印预计将要创建的Hash Map的大小信息。
        System.out.println("Creating storage map of size = " + estimatedNumberOfRecords);
        // 初始化 LinkedHashMap。此处指定了初始容量、加载因子（0.75，这是默认值，用于平衡时间和空间效率）和访问顺序（true 表示按访问顺序排序，这对于实现LRU缓存很有用）。
        storage = new LinkedHashMap<Integer, List<Integer>>(estimatedNumberOfRecords, 0.75f, true);
        // 记录下开始创建数据前的可用内存量，用于后续计算内存使用情况。
        long freeMemoryAtStart = Runtime.getRuntime().freeMemory();
        // 初始化键值 key，用于 LinkedHashMap 的键
        int key = 0;
        // 开始一个循环，条件是键值小于 Integer.MAX_VALUE。这是为了确保不会超出整数的最大值，防止溢出
        while (key < Integer.MAX_VALUE) {
            // 在循环中检查当前可用内存是否小于预留的10GB。如果小于，表明内存紧张，需要采取措施。
            // 如果可用内存不足，调用垃圾收集器尝试回收内存，并打印垃圾回收后的内存状态。
            if (Runtime.getRuntime().freeMemory() < RESERVED_SPACE) {
                System.out.println("Garbage collecting...");
                System.gc();
                System.out.println("Finished; memory remaining: " + Runtime.getRuntime().freeMemory() + " bytes");
                // 如果垃圾回收后内存仍然不足，则跳出循环，停止数据的创建。
                if (Runtime.getRuntime().freeMemory() < RESERVED_SPACE) {
                    break;
                }
            }
            // 创建一个列表对象
            List<Integer> integers = createIntegers();
            // 存储在 LinkedHashMap 中，然后键值 key 自增
            storage.put(key++, integers);
            if (key % 100000 == 0) {
                // 每创建10万条记录，调用 printMemoryUsed 方法打印当前的内存使用情况
                printMemoryUsed(key, freeMemoryAtStart);
            }
        }
        // 循环结束后，再次打印内存使用情况
        printMemoryUsed(key, freeMemoryAtStart);

        System.out.println("DONE!");

        return key;

    }

    private void printMemoryUsed(long key, long freeMemoryAtStart) {
        // freeMemoryAtStart 是方法调用时传入的开始时间点的可用内存量
        // 而 Runtime.getRuntime().freeMemory() 是当前可用的内存量。
        long memoryUsed = freeMemoryAtStart - Runtime.getRuntime().freeMemory();
        // 计算程序从启动到现在的运行时间（单位为毫秒）
        long uptime = System.currentTimeMillis() - startTime;
        // 这是一条打印语句，输出以下信息：
        //  当前日期和时间（通过 getDate() 方法获取）。
        //  到目前为止创建的键的数量（key 参数）。
        //  使用的内存量，转换为兆字节（MB）。
        //  每条记录平均使用的字节数（总内存使用量除以键的数量）。
        //  当前的可用内存量，也转换为兆字节（MB）。
        //  保留的内存量（RESERVED_SPACE 常量，已设置为10GB）。
        //  程序的运行时间，转换为秒。
        System.out.println(getDate() + "Created " + key + " keys, using " + (memoryUsed / 1024 / 1024) + "MB; that's " + (memoryUsed / key)
                + " bytes per record; free memory = " + (Runtime.getRuntime().freeMemory() / 1024 / 1024) + "MB" + "; reserved memory = " + RESERVED_SPACE
                + " bytes; uptime = " + (uptime / 1000) + " seconds");
    }

    private List<Integer> createIntegers() {
        // 这行代码生成一个随机的整数，用作列表的大小。
        int listSize = RANDOM.nextInt(MAX_LIST_SIZE);
        //  LinkedList 对象，用于存储整数
        LinkedList<Integer> integerList = new LinkedList<Integer>();
        // 生成一个随机整数，并将这个整数添加到 integerList 链表的尾部。
        for (int i = 0; i < listSize; i++) {
            integerList.add(RANDOM.nextInt());
        }
        return integerList;
    }

    private static final class Statistics {
        // 使用 AtomicLong 来记录缓存命中(hits)和未命中(misses)的次数。AtomicLong 提供了线程安全的操作来保证在多线程环境中数据的一致性
        private final AtomicLong hits = new AtomicLong();
        private final AtomicLong misses = new AtomicLong();
        // 记录操作中所花费时间的最大值（以纳秒为单位）。这可以用来监测性能瓶颈
        private long maxTimeTakenNanos = 0;
        // 在 Statistics 构造函数中，初始化一个新的线程用于定期报告统计数据
        public Statistics() {
            Thread thread = new Thread() {

                @Override
                public void run() {
                    while (true) {
                        // 这个线程会每秒醒来一次，执行一次数据报告。
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {

                            // 捕获 InterruptedException 并设置线程的中断状态，同时使用 synchronized 块确保对统计数据的访问是线程安全的。
                            Thread.currentThread().interrupt();
                        }
                        synchronized (Statistics.this) {

                            // 打印当前的统计数据，包括命中率、命中总次数、未命中总次数、操作总次数、剩余内存量、操作的最大耗时以及程序的运行时间。
                            // 之后重置 hits 和 misses 以准备下一次统计。
                            long uptime = System.currentTimeMillis() - startTime;
                            if (hits.get() > 0 && misses.get() > 0) {
                                System.out.println(getDate() + "Hit rate = " + ((double)hits.get() / ((double)misses.get() + (double)hits.get())) 
                                                + "; total hits = " + hits.get()
                                                + "; total misses = " + misses.get() 
                                                + "; total reads = " + (hits.get() + misses.get())
                                                + "; free memory = " + (Runtime.getRuntime().freeMemory() / 1024 / 1024) + "MB"
                                                + "; max time taken = " + ((double)maxTimeTakenNanos / 1000 / 1000) + "ms"
                                        		+ "; uptime = " + (uptime / 1000) + " seconds");
                            }
                            hits.set(0);
                            misses.set(0);
                        }
                    }
                }
                
            };
            // 启动这个线程。这个线程将持续运行并每秒输出一次统计数据。
            thread.start();
        }
        // public synchronized void registerHit(): 这个方法用于每次缓存命中时调用，它通过 AtomicLong 类的 getAndIncrement() 方法安全地增加 hits 的计数。
        public synchronized void registerHit() {
            hits.getAndIncrement();
        }
        // public synchronized void registerMiss(): 当缓存访问未命中时调用此方法，它同样利用 AtomicLong 的 getAndIncrement() 方法递增 misses 计数器
        public synchronized void registerMiss() {
            misses.getAndIncrement();
        }
        // 确保了在并发环境下记录的最大时间是准确的。
        public synchronized void recordTimeTaken(long timeTakenNanos) {
            if (timeTakenNanos > maxTimeTakenNanos) {
                maxTimeTakenNanos = timeTakenNanos;
            }
        }

    }
    // getDate() 方法提供了一个线程安全的方式来获取当前时间的格式化表示，适用于日志记录或任何需要时间戳的场景。
    private static synchronized String getDate() {
        return DATE_FORMAT.format(new Date(System.currentTimeMillis())) + " - ";
    }

}


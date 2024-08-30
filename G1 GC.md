# Garbage-First学习

​	非压缩垃圾收集器**non-compacting Garbage Collector (GC)**的常见情况：假设年轻代中的分配失败触发了年轻代收集，从而导致晋升到老一代。此外，假设碎片化的老一代没有足够的空间容纳新晋升的对象。这种情况将触发完整的垃圾收集周期（Full GC），这将执行堆的压缩。它会尝试使用串行（单线程）完整垃圾收集Full GC（即停止世界 (STW)）来回收其碎片堆。

- 使用 CMS GC，整个收集过程是串行且 STW 的，因此您的`应用程序线程会在整个过程中停止，同时回收并压缩堆空间`。STW 暂停的持续时间取决于您的堆大小和幸存的对象。
- 在并行GC（Parallel GC、Serial GC）中，垃圾收集器在处理老年代的垃圾收集时，会执行一个大的STW暂停，整个老年代的回收会在这个暂停期间一次性完成，而不是增量式地分阶段进行。这意味着在这个STW暂停期间，`应用程序的所有线程都被暂停`，直到垃圾收集完成。

​	G1是一种**服务器端的垃圾收集器**，应用在多处理器和大容量内存环境中，在实现高吞吐量的同时，尽可能的满足垃圾收集暂停时间的要求。

​	G1 是一种分代、增量、并行、大多数情况下并发、STW和清除垃圾收集器，它监视每个目标STW中的暂停时间。与其他收集器类似，G1 将堆分为（虚拟的）年轻代和（虚拟的）老年代。空间回收工作集中在最高效的年轻代上，偶尔也会在老年代进行空间回收。

​	



## 目标与功能设计

​	由此，引出G1 GC的**目标**是：G1 旨在使用当前的目标应用程序和环境在`延迟和吞吐量`之间实现最佳平衡。

### 设计目标

​	G1收集器的**设计目标**是取代CMS收集器，它**同CMS相比，在以下方面表现的更出色**： 

1. G1是一个有整理内存过程的垃圾收集器，不会产生很多内存碎片。

2.  G1的Stop The World(STW)更可控，G1在停顿时间上添加了预测机制，用户可以指定期望停顿时间。

### 针对场景

它是专门**针对以下应用场景设计**的: 

1. 像CMS收集器一样，能`与应用程序线程并发`执行。
2. 整理空闲空间更快。，是指垃圾收集器能够更迅速地压缩和整理内存中的空闲空间，能够更有效地应对`内存碎片化`问题。
   - G1尤其重要的特性，使得Java堆中始终保持足够的连续空间供对象分配。这种操作通常称为`“内存压缩”或“内存整理”`。这在需要频繁分配大对象的应用程序中尤为重要。
3. 需要GC**停顿时间更好预测**，允许用户设置一个最大暂停时间目标，例如200毫秒。
4. **不希望牺牲大量的吞吐性能**，设计目标之一是在提供低停顿时间的同时，尽可能保持较高的吞吐量。
5. **不需要更大的Java Heap**，G1 GC能够在不增加堆内存的前提下，仍然提供良好的垃圾收集性能。

### 功能

其**功能包括**：

1. 堆大小高达数十GB 或更大，其中超过 50% 的 Java 堆被`实时数据`占用。
   - 通过其`分区式管理`（Region-based Management）和`并行、并发收集机制`和`压缩与整理`，避免了传统垃圾收集器在大堆中出现的`长时间停顿问题`。
2. 对象分配和提升的速度可能会随着时间的推移而发生很大变化。
   - 动态调整堆区域（Region）的分配，G1 GC会根据对象分配和提升的速度，动态调整年轻代和老年代所占的Region数量。
3. 堆中存在大量碎片。
   - 大型应用程序中，某些对象可能会在内存中`保留很长`（实时数据）时间。当这些对象被频繁分配并释放时，可能会导致内存空间被碎片化。
4. 可预测的暂停时间目标不超过几百毫秒，避免长时间的垃圾收集暂停。

### 重要词

**Region**：

​	而G1的各代存储地址是不连续的，每一代都使用了n个不连续的大小相同的Region，每个Region占有一块连续的虚拟内存地址。如下图所示：

<img src="D:\Study\javaStudy\img\8ca16868.png" alt="8ca16868" style="zoom:50%;" />

​	在上图中，我们注意到还有一些Region标明了H，它代表Humongous，这表示这些Region存储的是巨大对象（humongous object，H-obj），即大小大于等于region一半的对象。H-obj有如下几个特征：

- H-obj直接分配到了old gen，防止了反复拷贝移动。
- H-obj在global concurrent marking阶段的cleanup 和 full GC阶段回收。
- 在分配H-obj之前先检查是否超过 `initiating heap occupancy percent`和`the marking threshold`, 如果超过的话，就启动`global concurrent marking`，为的是提早回收，防止 evacuation failures 和 full GC。

​	**实操**：为了减少连续H-objs分配对GC的影响，需要把大对象变为普通的对象，建议增大Region size。一个Region的大小可以通过参数`-XX:G1HeapRegionSize`设定，取值范围从1M到32M，且是2的指数。如果不设定，那么G1会根据Heap大小自动决定。

**SATB**：

​	SATB（Snapshot-At-The-Beginning）是一种用于并发垃圾收集的标记算法，特别适用于G1 GC。它的主要作用是确保在并发垃圾收集过程中，即使**应用程序线程继续运行并且对象状态发生变化，垃圾收集器仍然能够正确地标记和回收对象**。

​	**那么它是怎么维持并发GC的正确性的呢？**

​	根据**三色标记算法**，我们知道对象存在三种状态：

- 白：对象没有被标记到，标记阶段结束后，会被当做垃圾回收掉。
- 灰：对象被标记了，但是它的field还没有被标记或标记完。
- 黑：对象被标记了，且它的所有field也被标记完了。

​	由于并发阶段的存在，Mutator和Garbage Collector线程同时对对象进行修改，就会出现白对象漏标的情况，这种情况发生需要**同时满足**两个情况：

- Mutator赋予一个黑对象该白对象的引用。
- Mutator删除了所有从灰对象到该白对象的直接或者间接引用。

​	对于第一个条件，在并发标记阶段，如果该白对象是new出来的，并没有被灰对象持有，那么它会不会被漏标呢？Region中有两个top-at-mark-start（TAMS）指针，分别为prevTAMS和nextTAMS。在TAMS以上的对象是新分配的，这是一种隐式的标记。对于在GC时已经存在的白对象，如果它是活着的，它必然会被另一个对象引用，即条件二中的灰对象。如果灰对象到白对象的直接引用或者间接引用被替换了，或者删除了，白对象就会被漏标，从而导致被回收掉，这是非常严重的错误，所以**SATB通过write barrier（写屏障）破坏了第二个条件**。也就是说，一个对象的引用被替换时，可以通过write barrier（写屏障）将旧引用记录下来。

​	SATB也是有副作用的，如果被替换的白对象就是要被收集的垃圾，这次的标记会让它躲过GC，这就是float garbage。因为SATB的做法精度比较低，所以造成的float garbage也会比较多。

- **Float garbage**（浮动垃圾）指的是在垃圾收集过程中，本应该被回收的垃圾对象，因为某些原因没有被回收，从而在堆中继续占用内存的现象。



**RSet**：

​	全称是Remembered Set，是辅助GC过程的一种结构，典型的空间换时间工具，和Card Table有些类似。

​	还有一种数据结构也是辅助GC的：Collection Set（CSet），它记录了GC要收集的Region集合，集合里的Region可以是任意年代的。

​	在GC的时候，对于old->young和old->old的跨代对象引用，只要扫描对应的CSet中的RSet即可。 

​	逻辑上说每个Region都有一个RSet，RSet记录了其他Region中的对象引用本Region中对象的关系，属于points-into结构（谁引用了我的对象）。

​	而Card Table则是一种points-out（我引用了谁的对象）的结构，每个Card 覆盖一定范围的Heap（一般为512Bytes）。

​	G1的RSet是在Card Table的基础上实现的：每个Region会记录下别的Region有指向自己的指针，并标记这些指针分别在哪些Card的范围内。

​	 这个RSet其实是一个Hash Table，Key是别的Region的起始地址，Value是一个集合，里面的元素是Card Table的Index。

![5aea17be](img/5aea17be.png)

​	上图中有三个Region，每个Region被分成了多个Card，在不同Region中的Card会相互引用，Region1中的Card中的对象引用了Region2中的Card中的对象，`蓝色实线`表示的就是points-out的关系，而在Region2的RSet中，记录了Region1的Card，即`红色虚线`表示的关系，这就是points-into。 而维系RSet中的引用关系靠post-write barrier和Concurrent refinement threads来维护.

​	**RSet的引入**：post-write barrier记录了跨Region的引用更新，更新日志缓冲区则记录了那些包含更新引用的Cards。一旦缓冲区满了，Post-write barrier就停止服务了，会由Concurrent refinement threads处理这些缓冲区日志。 RSet究竟是怎么辅助GC的呢？在做YGC的时候，只需要选定young generation region的RSet作为根集，这些RSet记录了old->young的跨代引用，避免了扫描整个old generation。 而mixed gc的时候，old generation中记录了old->old的RSet，young->old的引用由扫描全部young generation region得到，这样也不用扫描全部old generation region。所以RSet的引入大大减少了GC的工作量。



**Pause Prediction Model**

​	Pause Prediction Model 即`停顿预测模型`。它在**G1中的作用**是：G1 使用暂停预测模型来满足用户定义的暂停时间目标，并根据指定的暂停时间目标选择要采集的区域数量。

​	GC是一个响应时间优先的GC算法，它与CMS最大的不同是，用户可以设定整个GC过程的期望停顿时间，参数-XX:MaxGCPauseMillis指定一个G1收集过程目标停顿时间，默认值200ms，不过它不是硬性条件，只是期望值。那么G1怎么满足用户的期望呢？就需要这个停顿预测模型了。G1根据这个模型统计计算出来的历史数据来预测本次收集需要选择的Region数量，从而尽量满足用户设定的目标停顿时间。 停顿预测模型是以衰减标准偏差为理论基础实现的：

```c++
//  share/vm/gc_implementation/g1/g1CollectorPolicy.hpp
double get_new_prediction(TruncatedSeq* seq) {
    return MAX2(seq->davg() + sigma() * seq->dsd(),
                seq->davg() * confidence_factor(seq->num()));
}
```

​	在这个预测计算公式中：davg表示衰减均值，sigma()返回一个系数，表示信赖度，dsd表示衰减标准偏差，confidence_factor表示可信度相关系数。而方法的参数TruncateSeq，顾名思义，是一个截断的序列，它只跟踪了序列中的最新的n个元素。

​	在G1 GC过程中，每个可测量的步骤花费的时间都会记录到TruncateSeq（继承了AbsSeq）中，用来计算衰减均值、衰减变量，衰减标准偏差等：

```c++
// src/share/vm/utilities/numberSeq.cpp

void AbsSeq::add(double val) {
  if (_num == 0) {
    // if the sequence is empty, the davg is the same as the value
    _davg = val;
    // and the variance is 0
    _dvariance = 0.0;
  } else {
    // otherwise, calculate both
    _davg = (1.0 - _alpha) * val + _alpha * _davg;
    double diff = val - _davg;
    _dvariance = (1.0 - _alpha) * diff * diff + _alpha * _dvariance;
  }
}
```

​	比如要预测一次GC过程中，RSet的更新时间，这个操作主要是将Dirty Card加入到RSet中，具体原理参考前面的RSet。每个Dirty Card的时间花费通过_cost_per_card_ms_seq来记录，具体预测代码如下：

```c++
//  share/vm/gc_implementation/g1/g1CollectorPolicy.hpp

 double predict_rs_update_time_ms(size_t pending_cards) {
    return (double) pending_cards * predict_cost_per_card_ms();
 }
 double predict_cost_per_card_ms() {
    return get_new_prediction(_cost_per_card_ms_seq);
 }
```

​	get_new_prediction就是我们开头说的方法，现在大家应该基本明白停顿预测模型的实现原理了。



### 工作原理

​	G1 GC 算法确实利用了 HotSpot 的一些基本概念。 例如，分配、复制到存活空间和晋升到旧一代的概念与以前的 HotSpot GC 实现类似。 

​	Eden区域和survivor区域仍然构成年轻一代。 除了 "humongous巨型 "分配外，大多数分配都在Eden中进行。

​	G1 GC 会根据暂停时间目标选择自适应的年轻代大小。 年轻一代的大小可以从预设的最小值到预设的最大值不等，JVM参数设置。 当 Eden 达到容量时，就会出现 `"年轻垃圾收集"，Young GC也称为 "疏散暂停"`。 这是一种 STW 暂停，它将从组成 Eden 的区域中复制（疏散）实时对象到 "to-space "的幸存者区域。虽然这是一个停顿事件，但G1 GC的**设计目标是尽量缩短这个停顿时间，以减少对应用程序的影响**。

​	在“撤离暂停 Young GC”期间，G1 GC会将Eden区域中仍然存活的对象复制到Survivor区域，称为“to-space”的Survivor区域。这些对象将在Survivor区域中继续存在，如果在后续垃圾收集中仍然存活，根据对象的年龄和老年代阈值，它们可能会被提升到老年代。



​	**G1提供了两种GC模式**，Young GC和Mixed GC，两种都是完全Stop The World的。

- Young GC：选定所有年轻代里的Region。通过控制年轻代的region个数，即年轻代内存大小，来控制young GC的时间开销。
- Mixed GC：选定所有年轻代里的Region，外加根据global concurrent marking统计得出`收集收益高`的若干老年代Region。在用户指定的开销目标范围内尽可能选择收益高的老年代Region。

​	由上面的描述可知，**Mixed GC不是full GC**，它只能回收部分老年代的Region，如果mixed GC实在无法跟上程序分配内存的速度，导致老年代填满无法继续进行Mixed GC，就会使用serial old GC（full GC）来收集整个GC heap。所以我们可以知道，G1是不提供full GC的。



​	**global concurrent marking**：它的执行过程类似CMS，但是不同的是，在G1 GC中，它主要是为Mixed GC提供标记服务的，并不是一次GC过程的一个必须环节。global concurrent marking的执行过程分为**四个步骤**：

1. 初始标记（initial mark，STW）。它标记了从GC Root开始直接可达的对象。
2. 并发标记（Concurrent Marking）。这个阶段从GC Root开始对heap中的对象标记，标记线程与应用程序线程并行执行，并且收集各个Region的存活对象信息。
3. 最终标记（Remark，STW）。标记那些在并发标记阶段发生变化的对象，将被回收。
4. 清除垃圾（Cleanup）。清除空Region（没有存活对象的），加入到free list。

​	第一阶段initial mark是共用了Young GC的暂停，这是因为他们可以复用root scan操作，所以可以说**global concurrent marking是伴随Young GC而发生**的。第四阶段Cleanup只是回收了没有存活对象的Region，所以它并不需要STW。



​	Young GC发生的时机大家都知道，那什么**时候发生Mixed GC呢**？其实是由一些参数控制着的，另外也控制着哪些老年代Region会被选入CSet。 

​	` G1HeapWastePercent`：在global concurrent marking结束之后，我们可以知道old gen regions中有多少空间要被回收，在**每次YGC之后和再次发生Mixed GC之前**，会检查垃圾占比是否达到此参数，只有达到了，下次才会发生Mixed GC。 

​	`G1MixedGCLiveThresholdPercent`：old generation region中的存活对象的占比，只有在此参数之下，才会被选入CSet。 

​	`G1MixedGCCountTarget`：一次global concurrent marking之后，最多执行Mixed GC的次数。

​	`G1OldCSetRegionThresholdPercent`：一次Mixed GC中能被选入CSet的最多old generation region数量。

​	除了以上的参数，G1 GC相关的其他主要的参数有

| 参数                               | 含义                                                         |
| :--------------------------------- | :----------------------------------------------------------- |
| -XX:G1HeapRegionSize=n             | 设置Region大小，并非最终值                                   |
| -XX:MaxGCPauseMillis               | 设置G1收集过程目标时间，默认值200ms，不是硬性条件            |
| -XX:G1NewSizePercent               | 新生代最小值，默认值5%                                       |
| -XX:G1MaxNewSizePercent            | 新生代最大值，默认值60%                                      |
| -XX:ParallelGCThreads              | STW期间，并行GC线程数                                        |
| -XX:ConcGCThreads=n                | 并发标记阶段，并行执行的线程数                               |
| -XX:InitiatingHeapOccupancyPercent | 设置触发标记周期的 Java 堆占用率阈值。默认值是45%。这里的java堆占比指的是non_young_capacity_bytes，包括old+humongous |
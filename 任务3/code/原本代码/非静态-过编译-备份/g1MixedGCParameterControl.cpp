// #include "precompiled.hpp"
#include "gc/g1/g1CollectedHeap.inline.hpp"
// #include "gc/g1/g1MixedGCThresholdControl.hpp"
// #include "gc/g1/g1Predictions.hpp"
// #include "gc/g1/g1Trace.hpp"
// #include "logging/log.hpp"


/*
G1AdaptiveParameterControl::G1AdaptiveParameterControl(
    uintx MGCT, uintx OCRTP,
    G1OldGenAllocationTracker const *old_gen_alloc_tracker,
    G1Predictions const *predictor)

    
    : _mixed_gc_count_target(MGCT), _old_cset_region_threshold_percent(OCRTP),
      _old_gen_alloc_tracker(old_gen_alloc_tracker),
      _predictor(predictor),
      _marking_times_s(10, 0.05),
      _allocation_rate_s(10, 0.05),
      _last_unrestrained_young_size(0){}
*/

G1AdaptiveParameterControl::G1AdaptiveParameterControl(
  uintx mixedGCLiveThresholdPercent, uintx heapWastePercent, uintx gcCountTarget, uintx csetPercent)
  : _heap_waste_percent(heapWastePercent),
    _mixed_gc_live_threshold_percent(mixedGCLiveThresholdPercent),
    _gc_count_target(gcCountTarget),
    _cset_percent(csetPercent){}



uintx G1AdaptiveParameterControl::getHeapWastePercent () const {
  return _heap_waste_percent;
}

uintx G1AdaptiveParameterControl::getGCLivePercent () const {
  return _mixed_gc_live_threshold_percent;
}

uintx G1AdaptiveParameterControl::getGCCountTarget () const {
  return _gc_count_target;
}

uintx G1AdaptiveParameterControl::getCSetPercent () const {
  return _cset_percent;
}
/*
  uint _candidate_region_min_safe_after_pruned = 10;
  uint _candidate_region_max_safe_after_pruned = 20;

  uintx _heap_waste_percent;
  uintx _mixed_gc_live_threshold_percent;
*/


 // 斩杀线
void G1AdaptiveParameterControl::updateGCLivePercent (size_t candidate_after_pruned) {
  if (candidate_after_pruned >= _candidate_region_min_safe_after_pruned
   && candidate_after_pruned <= _candidate_region_max_safe_after_pruned)
    {
        //printf("GC-Live-safe! \n");
        return;
    }

  if (candidate_after_pruned < _candidate_region_min_safe_after_pruned) {
    // uintx grade = (_candidate_region_min_safe_after_pruned - candidate_after_pruned + 4) / (5);
    // _mixed_gc_live_threshold_percent += 5 * grade;
    // printf("GC-Live < \n");
  } else {
    // uintx grade = (candidate_after_pruned - _candidate_region_max_safe_after_pruned + 4) / (5);
    // _mixed_gc_live_threshold_percent -= 5 * grade;
    // printf("GC-Live > \n");
  }
}

// HWP
void G1AdaptiveParameterControl::updateHeapWastePercent (size_t candidate_after_pruned) {
  if (candidate_after_pruned >= _candidate_region_min_safe_after_pruned 
   && candidate_after_pruned <= _candidate_region_max_safe_after_pruned)
    {
        // printf("HWP safe\n");
        return;
    }


  if (candidate_after_pruned < _candidate_region_min_safe_after_pruned) {
    uintx grade = (_candidate_region_min_safe_after_pruned - candidate_after_pruned + 4) / (5);
    _heap_waste_percent += 3 * grade;
    // if(_heap_waste_percent >= 3 * grade) _heap_waste_percent -= 3 * grade;
    // else _heap_waste_percent = 0;
    // printf("HWP <\n");
  } else {
    uintx grade = (candidate_after_pruned - _candidate_region_max_safe_after_pruned + 4) / (5);
    // _heap_waste_percent += 3 * grade;
    if(_heap_waste_percent >= 3 * grade) _heap_waste_percent -= 3 * grade;
    else _heap_waste_percent = 0;
    // printf("HWP > \n");
  }

}

//    _gc_count_target(gcCountTarget),
//      _cset_percent(csetPercent){}
void G1AdaptiveParameterControl::updateGCCountAndCsetPercent (size_t candidate_after_pruned) {
  if (candidate_after_pruned >= _candidate_region_min_safe_after_pruned
   && candidate_after_pruned <= _candidate_region_max_safe_after_pruned)
    {
        // printf("GC-Live-safe! \n");
        return;
    }

  if (candidate_after_pruned < _candidate_region_min_safe_after_pruned) {
    uintx grade = (_candidate_region_min_safe_after_pruned - candidate_after_pruned + 4) / (5);
    // _gc_count_target += 2 * grade;
    // _cset_percent -= 8 * grade;
    // printf("GC-Live < \n");

  } else {
    uintx grade = (candidate_after_pruned - _candidate_region_max_safe_after_pruned + 4) / (5);
    // _gc_count_target -= 2 * grade;
    // _cset_percent += 8 * grade;
    // printf("GC-Live > \n");
  }

}



/*
uintx G1AdaptiveParameterControl::G1AdaptiveParameterControl() {
  if (false && ((size_t)_marking_times_s.num() >= 1) &&
      ((size_t)_allocation_rate_s.num() >= 3) &&
      ((size_t)_garbage_rate_s.num() >= 1)) {
    double pred_marking_time = predict(&_marking_times_s);
    double pred_promotion_rate = predict(&_allocation_rate_s);
    double pred_garbage_rate = predict(&_garbage_rate_s);
    size_t pred_promotion_size = (size_t)(pred_marking_time * pred_promotion_rate);
    size_t predicted_needed_bytes_during_marking =
        pred_promotion_size +
        // In reality we would need the maximum size of the young gen during
        // marking. This is a conservative estimate.
        _last_unrestrained_young_size;
    // printf("Predicted marking time: %.3f\npredicted promotion rate: "
    //        "%.3f\npred_garbage_rate: %.3f\npredicted promotion size: "
    //        "%zu\npredicted needed bytes during marking: %zu\nused %zu\npromotion size %zu\ncapacity %zu\n",
    //        pred_marking_time, pred_promotion_rate, pred_garbage_rate,
    //        pred_promotion_size, predicted_needed_bytes_during_marking,
    //        G1CollectedHeap::heap()->used(), pred_promotion_size,
    //        G1CollectedHeap::heap()->max_capacity());

    // -- 缺少大括号
    if (pred_promotion_rate > pred_garbage_rate) {
      if(_mixed_gc_count_target > 1) _mixed_gc_count_target -= 1;
    }
    else
    {  if(_mixed_gc_count_target < 12) _mixed_gc_count_target += 1;}
    
    if (G1CollectedHeap::heap()->used() + pred_promotion_size >
        G1CollectedHeap::heap()->max_capacity()) 
      _mixed_gc_count_target = 1;
  }
  printf("Mixed GC count target: %zu\n", _mixed_gc_count_target);
  return _mixed_gc_count_target;
}

uintx G1AdaptiveMixedgcControl::getOldCSetRegionThresholdPercent() {
    if(false && ((size_t)_marking_times_s.num() >= 1) &&
         ((size_t)_allocation_rate_s.num() >= 3) && ((size_t)_garbage_rate_s.num() >= 1)){
    
        double pred_marking_time = predict(&_marking_times_s);      // 预计 - 标记用时

        double pred_promotion_rate = predict(&_allocation_rate_s);  // 预计 - 晋升速率
        
        double pred_garbage_rate = predict(&_garbage_rate_s);       // 预计 - 处理速率
        
                                                                    // 预计 - Marking 时候的老区晋升
        size_t pred_promotion_size = (size_t)(pred_marking_time * pred_promotion_rate);
                                                                    
                                                                    // 预计 - 总共需要的空间 = 老区 + 新区
        size_t predicted_needed_bytes_during_marking =
            pred_promotion_size +
            // In reality we would need the maximum size of the young gen during
            // marking. This is a conservative estimate.
            _last_unrestrained_young_size;
        // printf("Predicted marking time: %.3f\npredicted promotion rate: "
        //        "%.3f\npred_garbage_rate: %.3f\npredicted promotion size: "
        //        "%zu\npredicted needed bytes during marking: %zu\nused %zu\npromotion size %zu\ncapacity %zu\n",
        //        pred_marking_time, pred_promotion_rate, pred_garbage_rate,
        //        pred_promotion_size, predicted_needed_bytes_during_marking,
        //        G1CollectedHeap::heap()->used(), pred_promotion_size,
        //        G1CollectedHeap::heap()->max_capacity());

        //   ---  这里缺少大括号，有逻辑问题
        if (pred_promotion_rate > pred_garbage_rate) {               // 晋升速率 > 垃圾处理速率
                                                                    // 一次 最多 处理老区数  += 5
            if(_old_cset_region_threshold_percent < 100) _old_cset_region_threshold_percent += 5;
        }
        else {
            if(_old_cset_region_threshold_percent > 5) _old_cset_region_threshold_percent -= 5;
        }

        if (G1CollectedHeap::heap()->used() + pred_promotion_size >
        G1CollectedHeap::heap()->max_capacity()) 
            _old_cset_region_threshold_percent = 100;
    }
    printf("Old CSet region threshold percent: %zu\n", _old_cset_region_threshold_percent);
    return _old_cset_region_threshold_percent;
}

// 
void G1AdaptiveMixedgcControl::update_allocation_info(
    double allocation_time_s, size_t additional_buffer_size) {
  assert(allocation_time_s >= 0.0,
         "Allocation time must be positive but is %.3f", allocation_time_s);
  _last_allocation_time_s = allocation_time_s;
  _allocation_rate_s.add(last_mutator_period_old_allocation_rate());

  _last_unrestrained_young_size = additional_buffer_size;
}

void G1AdaptiveMixedgcControl::update_marking_length(double marking_length_s) {
   assert(marking_length_s >= 0.0, "Marking length must be larger than zero but is %.3f", marking_length_s);
  _marking_times_s.add(marking_length_s);
}

void G1AdaptiveMixedgcControl::update_garbage_info(double allocation_time_s, size_t garbage_bytes_s) {
  assert(allocation_time_s >= 0.0,
         "Allocation time must be positive but is %.3f", allocation_time_s);
  _last_allocation_time_s = allocation_time_s;
  _garbage_rate_s.add(garbage_bytes_s / _last_allocation_time_s);
}
double G1AdaptiveMixedgcControl::last_mutator_period_old_allocation_rate() const {
  assert(_last_allocation_time_s > 0, "This should not be called when the last GC is full");

  return _old_gen_alloc_tracker->last_period_old_gen_growth() / _last_allocation_time_s;
}

bool G1AdaptiveMixedgcControl::have_enough_data_for_prediction() const {
  return ((size_t)_marking_times_s.num() >= 3) &&
         ((size_t)_allocation_rate_s.num() >= 3);
}

double G1AdaptiveMixedgcControl::predict(TruncatedSeq const* seq) const {
  return _predictor->predict_zero_bounded(seq);
}*/

//public:
//  G1PruneRegionClosure(uint max_pruned, size_t max_wasted) :
//    _num_pruned(0), _cur_wasted(0), _max_pruned(max_pruned), _max_wasted(max_wasted) { }
//
//// if (_num_pruned > _max_pruned || _cur_wasted + reclaimable > _max_wasted)：
//// 判断是否已经修剪的区域数超过了_max_pruned，或者当前浪费字节数加上当前区域的可回收字节数是否超过了_max_wasted。如果是，返回true，表示停止修剪。
//  virtual bool do_heap_region(HeapRegion* r) {
//    size_t const reclaimable = r->reclaimable_bytes();
//    if (_num_pruned > _max_pruned ||
//        _cur_wasted + reclaimable > _max_wasted) {
//      return true;
//    }
//    r->rem_set()->clear(true /* cardset_only */);
//    _cur_wasted += reclaimable;
//    _num_pruned++;
//    return false;
//  }
//
//  uint num_pruned() const { return _num_pruned; }
//  size_t wasted() const { return _cur_wasted; }
//};
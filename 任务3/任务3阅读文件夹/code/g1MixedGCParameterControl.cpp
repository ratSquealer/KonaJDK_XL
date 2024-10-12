// #include "precompiled.hpp"
#include "gc/g1/g1CollectedHeap.inline.hpp"
// #include "gc/g1/g1Predictions.hpp"
// #include "gc/g1/g1Trace.hpp"
// #include "logging/log.hpp"


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
  } else {
    uintx grade = (candidate_after_pruned - _candidate_region_max_safe_after_pruned + 4) / (5);
    // _heap_waste_percent += 3 * grade;
    if(_heap_waste_percent >= 3 * grade) _heap_waste_percent -= 3 * grade;
    else _heap_waste_percent = 0;
  }

}

//    _gc_count_target(gcCountTarget),
//      _cset_percent(csetPercent){}

// 后两个参数
void G1AdaptiveParameterControl::updateGCCountAndCsetPercent (size_t candidate_after_pruned) {
  if (candidate_after_pruned >= _candidate_region_min_safe_after_pruned
   && candidate_after_pruned <= _candidate_region_max_safe_after_pruned)
    {
        // printf("GC-Live-safe! \n");
        return;
    }

  if (candidate_after_pruned < _candidate_region_min_safe_after_pruned) {
    uintx grade = (_candidate_region_min_safe_after_pruned - candidate_after_pruned + 4) / (5);

    // if (_gc_count_target >= 2 * grade)  _gc_count_target -= 2 * grade;
    // else _gc_count_target = 2;
    // _gc_count_target += 2 * grade;


    if (_cset_percent >= 3 * grade)  _cset_percent -= 3 * grade;
    else _cset_percent = 3;
    // _cset_percent += 6 * grade;



  } else {
    uintx grade = (candidate_after_pruned - _candidate_region_max_safe_after_pruned + 4) / (5);

    // if (_gc_count_target >= 2 * grade)  _gc_count_target -= 2 * grade;
    // else _gc_count_target = 2;
    // _gc_count_target += 2 * grade;


    // if (_cset_percent >= 3 * grade)  _cset_percent -= 3 * grade;
    // else _cset_percent = 3;
    _cset_percent +=  2 * grade;  if (_cset_percent > 25) _cset_percent = 20;

  }

}




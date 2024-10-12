#ifndef G1_MIXED_GC_PARAMETER_CONTROL_HPP
#define G1_MIXED_GC_PARAMETER_CONTROL_HPP
#include "gc/g1/g1OldGenAllocationTracker.hpp"
#include "memory/allocation.hpp"
#include "utilities/numberSeq.hpp"

class G1AdaptiveParameterControl {



  uint _candidate_region_min_safe_after_pruned = 10;
  uint _candidate_region_max_safe_after_pruned = 20;
  uintx _heap_waste_percent;
  uintx _mixed_gc_live_threshold_percent;
  uintx _gc_count_target;
  uintx _cset_percent;

  public:
    G1AdaptiveParameterControl (uintx mixedGCLiveThresholdPercent, uintx heapWastePercent, uintx gcCountTarget, uintx csetPercent);
    G1AdaptiveParameterControl ();


  virtual uintx getHeapWastePercent () const;
  virtual uintx getGCLivePercent () const;
  virtual uintx getGCCountTarget () const;
  virtual uintx getCSetPercent () const;
  virtual void updateGCLivePercent (size_t candidate_after_pruned);
  virtual void updateHeapWastePercent (size_t candidate_after_pruned);
  virtual void updateGCCountAndCsetPercent (size_t candidate_after_pruned);
};
#endif // G1_MIXED_GC_PARAMETER_CONTROL_HPP





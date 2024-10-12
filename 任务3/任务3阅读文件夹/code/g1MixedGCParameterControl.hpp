#ifndef G1_MIXED_GC_PARAMETER_CONTROL_HPP
#define G1_MIXED_GC_PARAMETER_CONTROL_HPP
#include "gc/g1/g1OldGenAllocationTracker.hpp"
#include "memory/allocation.hpp"
#include "utilities/numberSeq.hpp"

// 定义 G1AdaptiveMixedgcControl 类
class G1AdaptiveParameterControl {

  // uintx _mixed_gc_count_target;
  // uintx _old_cset_region_threshold_percent;

  uint _candidate_region_min_safe_after_pruned = 10;
  uint _candidate_region_max_safe_after_pruned = 20;
  uintx _heap_waste_percent;
  uintx _mixed_gc_live_threshold_percent;
  uintx _gc_count_target;
  uintx _cset_percent;

  public:
    G1AdaptiveParameterControl (uintx mixedGCLiveThresholdPercent, uintx heapWastePercent, uintx gcCountTarget, uintx csetPercent);
    G1AdaptiveParameterControl ();
  /*
  double _last_allocation_time_s;
  const G1OldGenAllocationTracker* _old_gen_alloc_tracker;
  const G1Predictions *_predictor;
  TruncatedSeq _marking_times_s;
  TruncatedSeq _allocation_rate_s;
  TruncatedSeq _garbage_rate_s;
  size_t _last_unrestrained_young_size;
  double predict(TruncatedSeq const *seq) const;
  bool have_enough_data_for_prediction() const;
  double last_mutator_period_old_allocation_rate() const;
 protected:
   virtual double last_marking_length_s() const {
     return _marking_times_s.last();
   }
 public:
  G1AdaptiveMixedgcControl(uintx MGCT, uintx OCRTP, G1OldGenAllocationTracker const* old_gen_alloc_tracker, G1Predictions const* predictor);
*/

/*
  virtual uintx getMixedGCCountTarget();
  virtual uintx getOldCSetRegionThresholdPercent();
  virtual void update_allocation_info(double allocation_time_s, size_t additional_buffer_size);
  virtual void update_marking_length(double marking_length_s);
  virtual void update_garbage_info(double allocation_time_s, size_t garbage_bytes_s);
*/  

  virtual uintx getHeapWastePercent () const;
  virtual uintx getGCLivePercent () const;
  virtual uintx getGCCountTarget () const;
  virtual uintx getCSetPercent () const;
  virtual void updateGCLivePercent (size_t candidate_after_pruned);
  virtual void updateHeapWastePercent (size_t candidate_after_pruned);
  virtual void updateGCCountAndCsetPercent (size_t candidate_after_pruned);
};
#endif // G1_MIXED_GC_PARAMETER_CONTROL_HPP





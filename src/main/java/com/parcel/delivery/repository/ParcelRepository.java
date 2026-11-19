package com.parcel.delivery.repository;

import com.parcel.delivery.entity.Parcel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParcelRepository extends JpaRepository<Parcel, Long> {

	List<Parcel> findByBatchIdOrderByStopOrderAscIdAsc(Long batchId);
}

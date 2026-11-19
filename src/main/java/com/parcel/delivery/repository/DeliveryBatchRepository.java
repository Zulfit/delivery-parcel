package com.parcel.delivery.repository;

import com.parcel.delivery.entity.DeliveryBatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryBatchRepository extends JpaRepository<DeliveryBatch, Long> {
}

package com.parcel.delivery.controller;

import com.parcel.delivery.dto.BatchResponse;
import com.parcel.delivery.dto.CreateBatchRequest;
import com.parcel.delivery.service.DeliveryService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/batches")
public class DeliveryBatchController {

	private final DeliveryService deliveryService;

	public DeliveryBatchController(DeliveryService deliveryService) {
		this.deliveryService = deliveryService;
	}

	@PostMapping
	public BatchResponse createBatch(@RequestBody(required = false) CreateBatchRequest request) {
		return deliveryService.createBatch(request == null ? new CreateBatchRequest() : request);
	}

	@GetMapping("/{id}")
	public BatchResponse getBatch(@PathVariable Long id) {
		return deliveryService.getBatch(id);
	}

	@PostMapping(path = "/{id}/parcels", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public BatchResponse uploadParcels(
			@PathVariable Long id,
			@RequestPart("files") List<MultipartFile> files) {
		return deliveryService.uploadParcels(id, files);
	}

	@PostMapping("/{id}/plan")
	public BatchResponse planBatch(@PathVariable Long id) {
		return deliveryService.planBatch(id);
	}
}

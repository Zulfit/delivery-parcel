package com.parcel.delivery.service;

import com.parcel.delivery.config.DeliveryProperties;
import com.parcel.delivery.dto.BatchResponse;
import com.parcel.delivery.dto.CreateBatchRequest;
import com.parcel.delivery.dto.HubStopResponse;
import com.parcel.delivery.dto.LabelParseResult;
import com.parcel.delivery.dto.ParcelResponse;
import com.parcel.delivery.entity.DeliveryBatch;
import com.parcel.delivery.entity.Parcel;
import com.parcel.delivery.exception.ResourceNotFoundException;
import com.parcel.delivery.repository.DeliveryBatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DeliveryService {

	private final DeliveryBatchRepository batchRepository;
	private final DeliveryProperties properties;
	private final OcrService ocrService;
	private final LabelParser labelParser;
	private final RouteSorter routeSorter;
	private final Path uploadRoot;

	public DeliveryService(
			DeliveryBatchRepository batchRepository,
			DeliveryProperties properties,
			OcrService ocrService,
			LabelParser labelParser,
			RouteSorter routeSorter) throws IOException {
		this.batchRepository = batchRepository;
		this.properties = properties;
		this.ocrService = ocrService;
		this.labelParser = labelParser;
		this.routeSorter = routeSorter;
		this.uploadRoot = Path.of(properties.getUploadDir()).toAbsolutePath().normalize();
		Files.createDirectories(uploadRoot);
	}

	@Transactional
	public BatchResponse createBatch(CreateBatchRequest request) {
		DeliveryBatch batch = new DeliveryBatch();
		batch.setHubName(firstNonBlank(request == null ? null : request.getHubName(), properties.getHub().getName()));
		batch.setHubLat(request != null && request.getHubLat() != null
				? request.getHubLat()
				: properties.getHub().getLat());
		batch.setHubLng(request != null && request.getHubLng() != null
				? request.getHubLng()
				: properties.getHub().getLng());
		batch.setStatus("OPEN");
		return toResponse(batchRepository.save(batch));
	}

	@Transactional(readOnly = true)
	public BatchResponse getBatch(Long id) {
		return toResponse(findBatch(id));
	}

	@Transactional
	public BatchResponse uploadParcels(Long batchId, List<MultipartFile> files) {
		if (files == null || files.isEmpty()) {
			throw new IllegalArgumentException("At least one image file is required");
		}
		DeliveryBatch batch = findBatch(batchId);
		if ("PLANNED".equals(batch.getStatus())) {
			throw new IllegalStateException("Batch already planned. Create a new batch to upload more parcels.");
		}

		Path batchDir = uploadRoot.resolve("batch-" + batchId);
		try {
			Files.createDirectories(batchDir);
		}
		catch (IOException e) {
			throw new IllegalStateException("Cannot create upload directory", e);
		}

		for (MultipartFile file : files) {
			if (file == null || file.isEmpty()) {
				continue;
			}
			String original = file.getOriginalFilename() == null ? "parcel.jpg" : file.getOriginalFilename();
			String storedName = UUID.randomUUID() + "-" + sanitizeFilename(original);
			Path destination = batchDir.resolve(storedName);
			try {
				Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
			}
			catch (IOException e) {
				throw new IllegalStateException("Failed to store " + original, e);
			}

			Parcel parcel = new Parcel();
			parcel.setOriginalFilename(original);
			parcel.setImagePath(destination.toString());
			batch.addParcel(parcel);
		}

		if (batch.getParcels().isEmpty()) {
			throw new IllegalArgumentException("No valid image files were uploaded");
		}

		return toResponse(batchRepository.save(batch));
	}

	@Transactional
	public BatchResponse planBatch(Long batchId) {
		DeliveryBatch batch = findBatch(batchId);
		if (batch.getParcels().isEmpty()) {
			throw new IllegalStateException("Upload at least one parcel photo before planning");
		}

		for (Parcel parcel : batch.getParcels()) {
			Path image = Path.of(parcel.getImagePath());
			String raw = ocrService.extractText(image);
			LabelParseResult parsed = labelParser.parse(raw);
			parcel.setOcrRawText(truncate(parsed.rawText(), 7900));
			parcel.setReceiverName(parsed.receiverName());
			parcel.setExtractedAddress(parsed.address());
			parcel.setPhone(parsed.phone());
			parcel.setPostcode(parsed.postcode());
			parcel.setAreaKey(parsed.areaKey());
		}

		routeSorter.assignStopOrder(batch.getParcels());
		batch.setStatus("PLANNED");
		return toResponse(batchRepository.save(batch));
	}

	private DeliveryBatch findBatch(Long id) {
		return batchRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + id));
	}

	private BatchResponse toResponse(DeliveryBatch batch) {
		List<Parcel> orderedParcels = batch.getParcels().stream()
				.sorted(Comparator
						.comparing(Parcel::getStopOrder, Comparator.nullsLast(Integer::compareTo))
						.thenComparing(Parcel::getId, Comparator.nullsLast(Long::compareTo)))
				.toList();

		List<ParcelResponse> parcelResponses = orderedParcels.stream().map(this::toParcelResponse).toList();
		HubStopResponse hub = toHub(batch);

		List<Object> suggestedRoute = new ArrayList<>();
		for (ParcelResponse parcel : parcelResponses) {
			Map<String, Object> stop = new LinkedHashMap<>();
			stop.put("type", "PARCEL");
			stop.put("stopOrder", parcel.stopOrder());
			stop.put("parcelId", parcel.id());
			stop.put("receiverName", parcel.receiverName());
			stop.put("address", parcel.extractedAddress());
			stop.put("phone", parcel.phone());
			stop.put("postcode", parcel.postcode());
			stop.put("areaKey", parcel.areaKey());
			suggestedRoute.add(stop);
		}

		Map<String, Object> hubStop = new LinkedHashMap<>();
		hubStop.put("type", hub.type());
		hubStop.put("stopOrder", parcelResponses.size() + 1);
		hubStop.put("name", hub.name());
		hubStop.put("lat", hub.lat());
		hubStop.put("lng", hub.lng());
		hubStop.put("mapsUrl", hub.mapsUrl());
		suggestedRoute.add(hubStop);

		return new BatchResponse(
				batch.getId(),
				batch.getCreatedAt(),
				batch.getStatus(),
				hub,
				parcelResponses,
				suggestedRoute
		);
	}

	private ParcelResponse toParcelResponse(Parcel parcel) {
		return new ParcelResponse(
				parcel.getId(),
				parcel.getOriginalFilename(),
				parcel.getImagePath(),
				parcel.getReceiverName(),
				parcel.getExtractedAddress(),
				parcel.getPhone(),
				parcel.getPostcode(),
				parcel.getAreaKey(),
				parcel.getStopOrder()
		);
	}

	private HubStopResponse toHub(DeliveryBatch batch) {
		return new HubStopResponse(
				"HUB",
				batch.getHubName(),
				batch.getHubLat(),
				batch.getHubLng(),
				"https://maps.google.com/?q=" + batch.getHubLat() + "," + batch.getHubLng()
		);
	}

	private String sanitizeFilename(String name) {
		return name.replaceAll("[^a-zA-Z0-9._-]", "_");
	}

	private String firstNonBlank(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value.trim();
	}

	private String truncate(String value, int max) {
		if (value == null) {
			return null;
		}
		return value.length() <= max ? value : value.substring(0, max);
	}
}

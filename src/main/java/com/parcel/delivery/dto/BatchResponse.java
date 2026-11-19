package com.parcel.delivery.dto;

import java.time.Instant;
import java.util.List;

public record BatchResponse(
		Long id,
		Instant createdAt,
		String status,
		HubStopResponse hub,
		List<ParcelResponse> parcels,
		List<Object> suggestedRoute
) {
}

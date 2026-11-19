package com.parcel.delivery.dto;

import com.parcel.delivery.geo.GeocodeStatus;

public record ParcelResponse(
		Long id,
		String originalFilename,
		String imagePath,
		String receiverName,
		String extractedAddress,
		String phone,
		String postcode,
		String areaKey,
		Integer stopOrder,
		Double lat,
		Double lng,
		GeocodeStatus geocodeStatus
) {
}

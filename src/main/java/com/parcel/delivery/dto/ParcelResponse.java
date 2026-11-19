package com.parcel.delivery.dto;

public record ParcelResponse(
		Long id,
		String originalFilename,
		String imagePath,
		String receiverName,
		String extractedAddress,
		String phone,
		String postcode,
		String areaKey,
		Integer stopOrder
) {
}

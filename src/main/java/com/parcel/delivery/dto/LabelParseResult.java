package com.parcel.delivery.dto;

public record LabelParseResult(
		String rawText,
		String receiverName,
		String address,
		String phone,
		String postcode,
		String areaKey
) {
}

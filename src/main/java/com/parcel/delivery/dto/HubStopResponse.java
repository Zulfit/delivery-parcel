package com.parcel.delivery.dto;

public record HubStopResponse(
		String type,
		String name,
		double lat,
		double lng,
		String mapsUrl
) {
}

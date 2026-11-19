package com.parcel.delivery.geo;

/**
 * {@code point} is non-null exactly when {@code status} is {@link GeocodeStatus#OK}.
 */
public record GeocodeResult(GeocodeStatus status, GeoPoint point, String providerQuery) {

	public GeocodeResult {
		if ((status == GeocodeStatus.OK) != (point != null)) {
			throw new IllegalArgumentException("point must be present exactly when status is OK");
		}
	}

	public static GeocodeResult ok(GeoPoint point, String providerQuery) {
		return new GeocodeResult(GeocodeStatus.OK, point, providerQuery);
	}

	public static GeocodeResult failed(String providerQuery) {
		return new GeocodeResult(GeocodeStatus.FAILED, null, providerQuery);
	}

	public static GeocodeResult skipped() {
		return new GeocodeResult(GeocodeStatus.SKIPPED, null, null);
	}
}

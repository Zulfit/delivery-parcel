package com.parcel.delivery.service;

import com.parcel.delivery.entity.Parcel;
import com.parcel.delivery.geo.GeoPoint;
import com.parcel.delivery.geo.GeocodeResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoutePlannerTest {

	private static final double HUB_LAT = 3.0;
	private static final double HUB_LNG = 101.4;

	private final RoutePlanner planner = new RoutePlanner();

	@Test
	void chainsGeocodedStopsByNearestNeighborFromHub() {
		Parcel near = geocoded("near", HUB_LAT, HUB_LNG + 0.01);
		Parcel westMid = geocoded("westMid", HUB_LAT, HUB_LNG - 0.015);
		Parcel eastFar = geocoded("eastFar", HUB_LAT, HUB_LNG + 0.02);

		List<Parcel> route = planner.assignStopOrder(List.of(westMid, eastFar, near), HUB_LAT, HUB_LNG);

		assertThat(names(route)).containsExactly("near", "eastFar", "westMid");
		assertThat(route).extracting(Parcel::getStopOrder).containsExactly(1, 2, 3);
	}

	@Test
	void ungeocodedStopsFollowGeocodedOnesGroupedByArea() {
		Parcel failed41200 = fallback("failed41200", "41200", GeocodeResult.failed("q"));
		Parcel skippedNoArea = fallback("skippedNoArea", null, GeocodeResult.skipped());
		Parcel geo = geocoded("geo", HUB_LAT + 0.01, HUB_LNG);
		Parcel skipped40100 = fallback("skipped40100", "40100", GeocodeResult.skipped());
		Parcel neverGeocoded = new Parcel();
		neverGeocoded.setReceiverName("neverGeocoded");
		neverGeocoded.setAreaKey("41200");

		List<Parcel> route = planner.assignStopOrder(
				List.of(failed41200, skippedNoArea, geo, skipped40100, neverGeocoded), HUB_LAT, HUB_LNG);

		assertThat(names(route)).containsExactly("geo", "skipped40100", "failed41200", "neverGeocoded", "skippedNoArea");
		assertThat(route).extracting(Parcel::getStopOrder).containsExactly(1, 2, 3, 4, 5);
	}

	@Test
	void withoutCoordinatesFallsBackToAreaKeyOrder() {
		Parcel a = fallback("a", " 41200 ", GeocodeResult.failed("q"));
		Parcel b = fallback("b", "40100", GeocodeResult.failed("q"));
		Parcel c = fallback("c", "", GeocodeResult.skipped());
		Parcel d = fallback("d", "41200", GeocodeResult.failed("q"));

		List<Parcel> route = planner.assignStopOrder(List.of(a, b, c, d), HUB_LAT, HUB_LNG);

		assertThat(names(route)).containsExactly("b", "a", "d", "c");
	}

	private Parcel geocoded(String name, double lat, double lng) {
		Parcel parcel = new Parcel();
		parcel.setReceiverName(name);
		parcel.applyGeocode(GeocodeResult.ok(new GeoPoint(lat, lng), "q"));
		return parcel;
	}

	private Parcel fallback(String name, String areaKey, GeocodeResult result) {
		Parcel parcel = new Parcel();
		parcel.setReceiverName(name);
		parcel.setAreaKey(areaKey);
		parcel.applyGeocode(result);
		return parcel;
	}

	private List<String> names(List<Parcel> parcels) {
		return parcels.stream().map(Parcel::getReceiverName).toList();
	}
}

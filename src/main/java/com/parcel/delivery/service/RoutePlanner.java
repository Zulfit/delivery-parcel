package com.parcel.delivery.service;

import com.parcel.delivery.entity.Parcel;
import com.parcel.delivery.geo.GeoPoint;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class RoutePlanner {

	private static final double EARTH_RADIUS_KM = 6371.0;

	/**
	 * Geocoded parcels first, as a nearest-neighbor tour from the hub. The rest follow,
	 * grouped by areaKey (alphabetical, UNKNOWN last, upload order within a group).
	 * Assigns stopOrder 1..N and returns parcels in that order.
	 */
	public List<Parcel> assignStopOrder(List<Parcel> parcels, double hubLat, double hubLng) {
		List<Parcel> uploadOrder = parcels.stream()
				.sorted(Comparator.comparing(Parcel::getId, Comparator.nullsLast(Long::compareTo)))
				.toList();

		List<Parcel> geo = new ArrayList<>();
		List<Parcel> fallback = new ArrayList<>();
		for (Parcel parcel : uploadOrder) {
			(parcel.geoPoint() != null ? geo : fallback).add(parcel);
		}

		List<Parcel> ordered = new ArrayList<>(nearestNeighborTour(geo, new GeoPoint(hubLat, hubLng)));
		ordered.addAll(groupByArea(fallback));

		int stop = 1;
		for (Parcel parcel : ordered) {
			parcel.setStopOrder(stop++);
		}
		return ordered;
	}

	private List<Parcel> nearestNeighborTour(List<Parcel> parcels, GeoPoint start) {
		List<Parcel> remaining = new ArrayList<>(parcels);
		List<Parcel> tour = new ArrayList<>();
		GeoPoint current = start;
		while (!remaining.isEmpty()) {
			GeoPoint from = current;
			Parcel next = remaining.stream()
					.min(Comparator.comparingDouble(p -> haversineKm(from, p.geoPoint())))
					.orElseThrow();
			remaining.remove(next);
			tour.add(next);
			current = next.geoPoint();
		}
		return tour;
	}

	private List<Parcel> groupByArea(List<Parcel> parcels) {
		Map<String, List<Parcel>> groups = new LinkedHashMap<>();
		for (Parcel parcel : parcels) {
			groups.computeIfAbsent(normalizeKey(parcel.getAreaKey()), ignored -> new ArrayList<>()).add(parcel);
		}
		return groups.keySet().stream()
				.sorted(Comparator.comparing((String k) -> "UNKNOWN".equals(k))
						.thenComparing(Comparator.naturalOrder()))
				.flatMap(key -> groups.get(key).stream())
				.toList();
	}

	private String normalizeKey(String areaKey) {
		if (areaKey == null || areaKey.isBlank()) {
			return "UNKNOWN";
		}
		return areaKey.trim().toUpperCase(Locale.ROOT);
	}

	private static double haversineKm(GeoPoint a, GeoPoint b) {
		double dLat = Math.toRadians(b.lat() - a.lat());
		double dLng = Math.toRadians(b.lng() - a.lng());
		double h = Math.pow(Math.sin(dLat / 2), 2)
				+ Math.cos(Math.toRadians(a.lat())) * Math.cos(Math.toRadians(b.lat())) * Math.pow(Math.sin(dLng / 2), 2);
		return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(h));
	}
}

package com.parcel.delivery.service;

import com.parcel.delivery.entity.Parcel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class RouteSorter {

	/**
	 * Groups parcels by areaKey (usually postcode), orders groups alphabetically,
	 * keeps upload order within a group, then assigns 1..N stopOrder.
	 */
	public List<Parcel> assignStopOrder(List<Parcel> parcels) {
		Map<String, List<Parcel>> groups = new LinkedHashMap<>();
		List<Parcel> sortedInput = parcels.stream()
				.sorted(Comparator.comparing(Parcel::getId, Comparator.nullsLast(Long::compareTo)))
				.toList();

		for (Parcel parcel : sortedInput) {
			String key = normalizeKey(parcel.getAreaKey());
			groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(parcel);
		}

		List<String> orderedKeys = groups.keySet().stream()
				.sorted(Comparator.comparing((String k) -> "UNKNOWN".equals(k))
						.thenComparing(Comparator.naturalOrder()))
				.toList();

		List<Parcel> ordered = new ArrayList<>();
		int stop = 1;
		for (String key : orderedKeys) {
			for (Parcel parcel : groups.get(key)) {
				parcel.setStopOrder(stop++);
				ordered.add(parcel);
			}
		}
		return ordered;
	}

	private String normalizeKey(String areaKey) {
		if (areaKey == null || areaKey.isBlank()) {
			return "UNKNOWN";
		}
		return areaKey.trim().toUpperCase(Locale.ROOT);
	}
}

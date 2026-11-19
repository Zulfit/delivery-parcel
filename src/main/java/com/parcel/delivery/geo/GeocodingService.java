package com.parcel.delivery.geo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.parcel.delivery.config.DeliveryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Locale;

/**
 * Geocodes addresses with OpenStreetMap Nominatim. Never throws: a miss is FAILED, nothing to look up is SKIPPED.
 */
@Service
public class GeocodingService {

	private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);
	private static final Duration MIN_REQUEST_INTERVAL = Duration.ofSeconds(1);

	private final DeliveryProperties.Geocoding config;
	private final RestClient restClient;
	private long nextRequestAtNanos = System.nanoTime();

	public GeocodingService(DeliveryProperties properties) {
		this.config = properties.getGeocoding();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(config.getTimeout()).build());
		requestFactory.setReadTimeout(config.getTimeout());
		this.restClient = RestClient.builder()
				.baseUrl(config.getBaseUrl())
				.requestFactory(requestFactory)
				.defaultHeader(HttpHeaders.USER_AGENT, config.getUserAgent())
				.build();
	}

	public GeocodeResult geocode(String address, String postcode) {
		if (!config.isEnabled() || address == null || address.isBlank()) {
			return GeocodeResult.skipped();
		}
		String query = buildQuery(address, postcode);
		try {
			NominatimPlace[] places = search(query);
			if (places == null || places.length == 0) {
				return GeocodeResult.failed(query);
			}
			GeoPoint point = new GeoPoint(Double.parseDouble(places[0].lat()), Double.parseDouble(places[0].lon()));
			return GeocodeResult.ok(point, query);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return GeocodeResult.failed(query);
		}
		catch (RuntimeException e) {
			log.warn("Geocoding failed for '{}': {}", query, e.getMessage());
			return GeocodeResult.failed(query);
		}
	}

	static String buildQuery(String address, String postcode) {
		String query = address.trim();
		if (postcode != null && !postcode.isBlank() && !query.contains(postcode.trim())) {
			query += ", " + postcode.trim();
		}
		if (!query.toLowerCase(Locale.ROOT).contains("malaysia")) {
			query += ", Malaysia";
		}
		return query;
	}

	private synchronized NominatimPlace[] search(String query) throws InterruptedException {
		long waitNanos = nextRequestAtNanos - System.nanoTime();
		if (waitNanos > 0) {
			Thread.sleep(Duration.ofNanos(waitNanos));
		}
		try {
			return restClient.get()
					.uri(uri -> {
						uri.path("/search")
								.queryParam("q", query)
								.queryParam("format", "jsonv2")
								.queryParam("limit", 1)
								.queryParam("countrycodes", "my");
						if (config.getEmail() != null && !config.getEmail().isBlank()) {
							uri.queryParam("email", config.getEmail());
						}
						return uri.build();
					})
					.retrieve()
					.body(NominatimPlace[].class);
		}
		finally {
			nextRequestAtNanos = System.nanoTime() + MIN_REQUEST_INTERVAL.toNanos();
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record NominatimPlace(String lat, String lon) {
	}
}

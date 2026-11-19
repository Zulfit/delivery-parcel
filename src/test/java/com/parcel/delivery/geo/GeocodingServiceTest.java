package com.parcel.delivery.geo;

import com.parcel.delivery.config.DeliveryProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class GeocodingServiceTest {

	private HttpServer server;

	@AfterEach
	void stopServer() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void disabledSkipsEveryAddress() {
		DeliveryProperties properties = new DeliveryProperties();
		properties.getGeocoding().setEnabled(false);

		GeocodeResult result = new GeocodingService(properties).geocode("No 1, Jalan Kenari, Klang", "41200");

		assertThat(result.status()).isEqualTo(GeocodeStatus.SKIPPED);
		assertThat(result.point()).isNull();
	}

	@Test
	void blankAddressIsSkipped() {
		GeocodingService service = new GeocodingService(propertiesFor("http://127.0.0.1:9"));

		assertThat(service.geocode("  ", "41200").status()).isEqualTo(GeocodeStatus.SKIPPED);
		assertThat(service.geocode(null, null).status()).isEqualTo(GeocodeStatus.SKIPPED);
	}

	@Test
	void unreachableProviderFailsSoftly() {
		GeocodingService service = new GeocodingService(propertiesFor("http://127.0.0.1:9"));

		GeocodeResult result = service.geocode("No 1, Jalan Kenari, Klang", "41200");

		assertThat(result.status()).isEqualTo(GeocodeStatus.FAILED);
		assertThat(result.providerQuery()).isEqualTo("No 1, Jalan Kenari, Klang, 41200, Malaysia");
	}

	@Test
	void parsesFirstNominatimHit() throws IOException {
		AtomicReference<String> query = new AtomicReference<>();
		AtomicReference<String> userAgent = new AtomicReference<>();
		String baseUrl = serve("""
				[{"place_id":1,"lat":"3.0441","lon":"101.4456","display_name":"Klang"}]
				""", query, userAgent);

		GeocodeResult result = new GeocodingService(propertiesFor(baseUrl)).geocode("Jalan Kenari, Klang, Malaysia", "41200");

		assertThat(result.status()).isEqualTo(GeocodeStatus.OK);
		assertThat(result.point()).isEqualTo(new GeoPoint(3.0441, 101.4456));
		assertThat(query.get()).contains("q=Jalan Kenari, Klang, Malaysia, 41200").contains("countrycodes=my");
		assertThat(userAgent.get()).isEqualTo("parcel-delivery-test");
	}

	@Test
	void emptyResultFails() throws IOException {
		String baseUrl = serve("[]", new AtomicReference<>(), new AtomicReference<>());

		GeocodeResult result = new GeocodingService(propertiesFor(baseUrl)).geocode("Nowhere road", null);

		assertThat(result.status()).isEqualTo(GeocodeStatus.FAILED);
		assertThat(result.point()).isNull();
	}

	private DeliveryProperties propertiesFor(String baseUrl) {
		DeliveryProperties properties = new DeliveryProperties();
		properties.getGeocoding().setBaseUrl(baseUrl);
		properties.getGeocoding().setUserAgent("parcel-delivery-test");
		properties.getGeocoding().setTimeout(Duration.ofSeconds(2));
		return properties;
	}

	private String serve(String body, AtomicReference<String> query, AtomicReference<String> userAgent) throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/search", exchange -> {
			query.set(URLDecoder.decode(exchange.getRequestURI().getRawQuery(), StandardCharsets.UTF_8));
			userAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, bytes.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(bytes);
			}
		});
		server.start();
		return "http://127.0.0.1:" + server.getAddress().getPort();
	}
}

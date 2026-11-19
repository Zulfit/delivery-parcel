package com.parcel.delivery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "delivery")
public class DeliveryProperties {

	private String uploadDir = "uploads";
	private final Hub hub = new Hub();
	private final Tesseract tesseract = new Tesseract();
	private final Geocoding geocoding = new Geocoding();

	public String getUploadDir() {
		return uploadDir;
	}

	public void setUploadDir(String uploadDir) {
		this.uploadDir = uploadDir;
	}

	public Hub getHub() {
		return hub;
	}

	public Tesseract getTesseract() {
		return tesseract;
	}

	public Geocoding getGeocoding() {
		return geocoding;
	}

	public static class Hub {
		private String name = "J&T Express-Klang Jaya (Pjs380)";
		private double lat = 3.010177;
		private double lng = 101.458588;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public double getLat() {
			return lat;
		}

		public void setLat(double lat) {
			this.lat = lat;
		}

		public double getLng() {
			return lng;
		}

		public void setLng(double lng) {
			this.lng = lng;
		}
	}

	public static class Tesseract {
		private String dataPath = "/opt/homebrew/share/tessdata";
		private String language = "eng";

		public String getDataPath() {
			return dataPath;
		}

		public void setDataPath(String dataPath) {
			this.dataPath = dataPath;
		}

		public String getLanguage() {
			return language;
		}

		public void setLanguage(String language) {
			this.language = language;
		}
	}

	public static class Geocoding {
		private boolean enabled = true;
		private String baseUrl = "https://nominatim.openstreetmap.org";
		private String userAgent = "parcel-delivery/0.0.1";
		private String email;
		private Duration timeout = Duration.ofSeconds(10);

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getBaseUrl() {
			return baseUrl;
		}

		public void setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
		}

		public String getUserAgent() {
			return userAgent;
		}

		public void setUserAgent(String userAgent) {
			this.userAgent = userAgent;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public Duration getTimeout() {
			return timeout;
		}

		public void setTimeout(Duration timeout) {
			this.timeout = timeout;
		}
	}
}

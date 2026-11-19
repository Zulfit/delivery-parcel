package com.parcel.delivery.dto;

public class CreateBatchRequest {

	private String hubName;
	private Double hubLat;
	private Double hubLng;

	public String getHubName() {
		return hubName;
	}

	public void setHubName(String hubName) {
		this.hubName = hubName;
	}

	public Double getHubLat() {
		return hubLat;
	}

	public void setHubLat(Double hubLat) {
		this.hubLat = hubLat;
	}

	public Double getHubLng() {
		return hubLng;
	}

	public void setHubLng(Double hubLng) {
		this.hubLng = hubLng;
	}
}

package com.parcel.delivery.entity;

import com.parcel.delivery.geo.GeoPoint;
import com.parcel.delivery.geo.GeocodeResult;
import com.parcel.delivery.geo.GeocodeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "parcels")
public class Parcel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "batch_id", nullable = false)
	private DeliveryBatch batch;

	@Column(nullable = false)
	private String imagePath;

	@Column(nullable = false)
	private String originalFilename;

	@Column(length = 8000)
	private String ocrRawText;

	private String receiverName;

	@Column(length = 1000)
	private String extractedAddress;

	private String phone;

	private String postcode;

	private String areaKey;

	private Integer stopOrder;

	private Double lat;

	private Double lng;

	@Enumerated(EnumType.STRING)
	private GeocodeStatus geocodeStatus;

	public Long getId() {
		return id;
	}

	public DeliveryBatch getBatch() {
		return batch;
	}

	public void setBatch(DeliveryBatch batch) {
		this.batch = batch;
	}

	public String getImagePath() {
		return imagePath;
	}

	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}

	public String getOriginalFilename() {
		return originalFilename;
	}

	public void setOriginalFilename(String originalFilename) {
		this.originalFilename = originalFilename;
	}

	public String getOcrRawText() {
		return ocrRawText;
	}

	public void setOcrRawText(String ocrRawText) {
		this.ocrRawText = ocrRawText;
	}

	public String getReceiverName() {
		return receiverName;
	}

	public void setReceiverName(String receiverName) {
		this.receiverName = receiverName;
	}

	public String getExtractedAddress() {
		return extractedAddress;
	}

	public void setExtractedAddress(String extractedAddress) {
		this.extractedAddress = extractedAddress;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getPostcode() {
		return postcode;
	}

	public void setPostcode(String postcode) {
		this.postcode = postcode;
	}

	public String getAreaKey() {
		return areaKey;
	}

	public void setAreaKey(String areaKey) {
		this.areaKey = areaKey;
	}

	public Integer getStopOrder() {
		return stopOrder;
	}

	public void setStopOrder(Integer stopOrder) {
		this.stopOrder = stopOrder;
	}

	public Double getLat() {
		return lat;
	}

	public Double getLng() {
		return lng;
	}

	public GeocodeStatus getGeocodeStatus() {
		return geocodeStatus;
	}

	/**
	 * Null unless the parcel geocoded successfully.
	 */
	public GeoPoint geoPoint() {
		return geocodeStatus == GeocodeStatus.OK ? new GeoPoint(lat, lng) : null;
	}

	public void applyGeocode(GeocodeResult result) {
		this.geocodeStatus = result.status();
		this.lat = result.point() == null ? null : result.point().lat();
		this.lng = result.point() == null ? null : result.point().lng();
	}
}

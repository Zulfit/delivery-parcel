package com.parcel.delivery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
}

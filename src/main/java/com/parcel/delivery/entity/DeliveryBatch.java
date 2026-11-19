package com.parcel.delivery.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "delivery_batches")
public class DeliveryBatch {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	@Column(nullable = false)
	private String status = "OPEN";

	@Column(nullable = false)
	private String hubName;

	@Column(nullable = false)
	private Double hubLat;

	@Column(nullable = false)
	private Double hubLng;

	@OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("stopOrder ASC, id ASC")
	private List<Parcel> parcels = new ArrayList<>();

	public Long getId() {
		return id;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

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

	public List<Parcel> getParcels() {
		return parcels;
	}

	public void setParcels(List<Parcel> parcels) {
		this.parcels = parcels;
	}

	public void addParcel(Parcel parcel) {
		parcels.add(parcel);
		parcel.setBatch(this);
	}
}

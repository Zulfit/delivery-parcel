package com.parcel.delivery;

import com.parcel.delivery.config.DeliveryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(DeliveryProperties.class)
public class ParcelDeliveryApplication {

	public static void main(String[] args) {
		SpringApplication.run(ParcelDeliveryApplication.class, args);
	}
}

package com.parcel.delivery.service;

import com.parcel.delivery.dto.LabelParseResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LabelParserTest {

	private final LabelParser parser = new LabelParser();

	@Test
	void prefersReceiverAddressAndPostcode() {
		String sample = """
				TikTok Shop
				J&T Express
				DROP-OFF
				680050441616912
				Sender
				Home Hydroponic
				HOME HYDROPONIC, 1337, Kampung Ru Dua, MARANG, TERENGGANU 21600
				Receiver
				Zulfitri Hakeem
				No 14, Lot 4563, Jalan Tanjung Kluang, Kg Jalan Kebun, Seksyen 30, SHAH ALAM, PETALING, SELANGOR 40460
				Order ID 586481062631146914
				""";

		LabelParseResult result = parser.parse(sample);

		assertEquals("40460", result.postcode());
		assertEquals("40460", result.areaKey());
		assertNotNull(result.address());
		assertTrue(result.address().toUpperCase().contains("SHAH ALAM")
				|| result.address().contains("40460")
				|| result.address().toUpperCase().contains("JALAN"));
		assertNotNull(result.receiverName());
	}

	@Test
	void parsesNoisyOcrReceiverInlineAndSortCode() {
		String noisy = """
				TikTok Shop
				680050441616912
				300-C01-PJ384 RB1
				Receiver Zulfer Hakeem
				DROP-OFF
				In transit by: 11/10/2026
				HYDROPONIC (FULLSET)
				""";

		LabelParseResult result = parser.parse(noisy);

		assertNotNull(result.receiverName());
		assertTrue(result.receiverName().toLowerCase().contains("hakeem"));
		assertEquals("PJ384", result.areaKey());
	}
}

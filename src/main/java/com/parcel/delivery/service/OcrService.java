package com.parcel.delivery.service;

import com.parcel.delivery.config.DeliveryProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Runs the system Tesseract binary (Homebrew). Optionally upscales via macOS sips first.
 */
@Service
public class OcrService {

	private final DeliveryProperties properties;
	private String tesseractBinary = "tesseract";

	public OcrService(DeliveryProperties properties) {
		this.properties = properties;
	}

	@PostConstruct
	void init() {
		Path brewBinary = Path.of("/opt/homebrew/bin/tesseract");
		if (Files.isExecutable(brewBinary)) {
			tesseractBinary = brewBinary.toString();
		}
		Path dataPath = Path.of(properties.getTesseract().getDataPath());
		if (!Files.isDirectory(dataPath)) {
			throw new IllegalStateException(
					"Tesseract data path not found: " + dataPath + ". Install with: brew install tesseract");
		}
	}

	public String extractText(Path imagePath) {
		Path workingImage = imagePath;
		Path tempUpscaled = null;
		try {
			tempUpscaled = upscaleIfPossible(imagePath);
			if (tempUpscaled != null) {
				workingImage = tempUpscaled;
			}

			List<String> attempts = new ArrayList<>();
			attempts.add(runTesseract(workingImage, "6"));
			attempts.add(runTesseract(workingImage, "4"));
			attempts.add(runTesseract(workingImage, "11"));

			return attempts.stream()
					.filter(text -> text != null && !text.isBlank())
					.max((a, b) -> Integer.compare(score(a), score(b)))
					.orElse("");
		}
		catch (Exception e) {
			if (e instanceof IllegalStateException ise) {
				throw ise;
			}
			throw new IllegalStateException("OCR failed for " + imagePath.getFileName() + ": " + e.getMessage(), e);
		}
		finally {
			if (tempUpscaled != null) {
				try {
					Files.deleteIfExists(tempUpscaled);
				}
				catch (Exception ignored) {
					// best effort cleanup
				}
			}
		}
	}

	private Path upscaleIfPossible(Path imagePath) {
		try {
			Path temp = Files.createTempFile("ocr-up-", ".png");
			ProcessBuilder pb = new ProcessBuilder(
					"sips",
					"-Z", "2000",
					imagePath.toAbsolutePath().toString(),
					"--out", temp.toAbsolutePath().toString()
			);
			pb.redirectErrorStream(true);
			Process process = pb.start();
			boolean finished = process.waitFor(30, TimeUnit.SECONDS);
			if (!finished || process.exitValue() != 0) {
				Files.deleteIfExists(temp);
				return null;
			}
			return temp;
		}
		catch (Exception e) {
			return null;
		}
	}

	private String runTesseract(Path imagePath, String psm) throws Exception {
		ProcessBuilder pb = new ProcessBuilder(
				tesseractBinary,
				imagePath.toAbsolutePath().toString(),
				"stdout",
				"-l", properties.getTesseract().getLanguage(),
				"--psm", psm
		);
		pb.redirectErrorStream(true);
		Process process = pb.start();
		String output;
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
			output = reader.lines().collect(Collectors.joining("\n"));
		}
		boolean finished = process.waitFor(60, TimeUnit.SECONDS);
		if (!finished) {
			process.destroyForcibly();
			return "";
		}
		if (process.exitValue() != 0) {
			return "";
		}
		return output == null ? "" : output.trim();
	}

	private int score(String text) {
		int score = text.length();
		String lower = text.toLowerCase();
		if (lower.contains("receiver")) {
			score += 200;
		}
		if (lower.contains("jalan") || lower.contains("selangor") || lower.contains("shah alam")) {
			score += 150;
		}
		if (text.matches("(?s).*\\b\\d{5}\\b.*")) {
			score += 100;
		}
		return score;
	}
}

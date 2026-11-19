package com.parcel.delivery.service;

import com.parcel.delivery.dto.LabelParseResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristics tuned for Malaysia J&amp;T / TikTok Shop AWB labels.
 * Prefers receiver block over sender when both appear.
 */
@Component
public class LabelParser {

	// Avoid matching 5 digits inside longer IDs (tracking / TikTok order numbers)
	private static final Pattern POSTCODE = Pattern.compile("(?<!\\d)(\\d{5})(?!\\d)");
	private static final Pattern PHONE = Pattern.compile("\\b(01[0-9][-\\s]?\\d{7,8})\\b");
	private static final Pattern TRACKING = Pattern.compile("\\b\\d{12,20}\\b");
	private static final Pattern SORT_CODE = Pattern.compile(
			"(?<![A-Za-z0-9])([A-Z]{1,3}\\d{3})(?![0-9])", Pattern.CASE_INSENSITIVE);
	private static final Pattern RECEIVER_INLINE = Pattern.compile(
			"(?i)receiver\\s*[:\\-]?\\s*(.+)");

	public LabelParseResult parse(String rawText) {
		if (rawText == null || rawText.isBlank()) {
			return new LabelParseResult("", null, null, null, null, "UNKNOWN");
		}

		List<String> lines = normalizeLines(rawText);
		String phone = findPhone(rawText);
		ReceiverBlock receiver = extractReceiver(lines);
		String postcode = receiver.postcode() != null ? receiver.postcode() : findBestPostcode(rawText, lines);
		String sortCode = findSortCode(rawText);
		String areaKey = postcode != null
				? postcode
				: (sortCode != null ? sortCode.toUpperCase(Locale.ROOT) : guessAreaKey(receiver.address()));

		return new LabelParseResult(
				rawText,
				receiver.name(),
				receiver.address(),
				phone,
				postcode,
				areaKey
		);
	}

	private List<String> normalizeLines(String rawText) {
		String[] split = rawText.split("\\R");
		List<String> lines = new ArrayList<>();
		for (String line : split) {
			String cleaned = line.replace('\u00A0', ' ').replace('|', ' ').trim();
			cleaned = cleaned.replaceAll("\\s{2,}", " ");
			if (!cleaned.isEmpty()) {
				lines.add(cleaned);
			}
		}
		return lines;
	}

	private ReceiverBlock extractReceiver(List<String> lines) {
		String inlineName = null;
		for (String line : lines) {
			Matcher m = RECEIVER_INLINE.matcher(line);
			if (m.find()) {
				String rest = m.group(1).trim();
				rest = rest.replaceAll("[—\\-]+$", "").trim();
				if (!rest.isBlank() && !rest.equalsIgnoreCase("name")) {
					inlineName = cleanupName(rest);
				}
				break;
			}
		}

		int receiverIdx = indexOfKeyword(lines, "receiver", "consignee", "to:");
		int senderIdx = indexOfKeyword(lines, "sender", "shipper", "from:");

		List<String> candidate;
		if (receiverIdx >= 0) {
			int end = lines.size();
			if (senderIdx > receiverIdx) {
				end = senderIdx;
			}
			end = Math.min(end, receiverIdx + 10);
			candidate = lines.subList(receiverIdx, end);
		}
		else if (senderIdx >= 0) {
			candidate = lines.subList(0, senderIdx);
			if (candidate.size() < 2) {
				candidate = lines.subList(Math.min(senderIdx + 1, lines.size()), lines.size());
			}
		}
		else {
			candidate = pickAddressLikeLines(lines);
		}

		String name = inlineName;
		List<String> addressParts = new ArrayList<>();
		String postcode = null;

		for (String line : candidate) {
			if (isNoise(line) && !line.toLowerCase(Locale.ROOT).contains("receiver")) {
				continue;
			}
			Matcher receiverLine = RECEIVER_INLINE.matcher(line);
			if (receiverLine.find()) {
				continue;
			}
			Matcher pc = POSTCODE.matcher(line);
			if (pc.find()) {
				postcode = pc.group(1);
			}
			if (name == null && looksLikePersonOrShopName(line)) {
				name = cleanupName(line);
				continue;
			}
			if (looksLikeAddress(line) || POSTCODE.matcher(line).find()) {
				addressParts.add(line);
			}
		}

		// Do NOT fall back to dumping raw OCR noise into the address field.
		addressParts.removeIf(this::isGarbageText);
		if (name != null && isGarbageText(name)) {
			name = null;
		}

		String address = addressParts.isEmpty() ? null : String.join(", ", addressParts);
		if (address != null && isGarbageText(address)) {
			address = null;
		}
		return new ReceiverBlock(name, address, postcode);
	}

	private String cleanupName(String value) {
		String cleaned = value.replaceAll("[^A-Za-z0-9 .'\\-]", " ").replaceAll("\\s{2,}", " ").trim();
		return cleaned.isBlank() || isGarbageText(cleaned) ? null : cleaned;
	}

	/** Detect OCR junk (symbols, sparse letters, nonsense). */
	private boolean isGarbageText(String text) {
		if (text == null || text.isBlank()) {
			return true;
		}
		String compact = text.replaceAll("\\s+", " ").trim();
		if (compact.length() < 6) {
			return true;
		}
		long letters = compact.chars().filter(Character::isLetter).count();
		long digits = compact.chars().filter(Character::isDigit).count();
		long weird = compact.chars()
				.filter(c -> !(Character.isLetterOrDigit(c) || Character.isWhitespace(c)
						|| c == ',' || c == '.' || c == '-' || c == '\'' || c == '/' || c == '#'))
				.count();
		double letterRatio = (double) letters / compact.length();
		double weirdRatio = (double) weird / compact.length();
		if (weirdRatio > 0.18) {
			return true;
		}
		if (letterRatio < 0.35 && digits < 5) {
			return true;
		}
		// Mostly short token salad without address keywords
		String lower = compact.toLowerCase(Locale.ROOT);
		boolean hasAddressWord = lower.contains("jalan") || lower.contains("jln") || lower.contains("taman")
				|| lower.contains("kampung") || lower.contains("kg ") || lower.contains("seksyen")
				|| lower.contains("selangor") || lower.contains("no ") || POSTCODE.matcher(compact).find();
		if (!hasAddressWord && compact.split("\\s+").length > 8 && letterRatio < 0.55) {
			return true;
		}
		return false;
	}

	private List<String> pickAddressLikeLines(List<String> lines) {
		List<String> result = new ArrayList<>();
		for (String line : lines) {
			if (looksLikeAddress(line) || POSTCODE.matcher(line).find()) {
				result.add(line);
			}
		}
		return result.isEmpty() ? lines : result;
	}

	private int indexOfKeyword(List<String> lines, String... keywords) {
		for (int i = 0; i < lines.size(); i++) {
			String lower = lines.get(i).toLowerCase(Locale.ROOT);
			for (String keyword : keywords) {
				if (lower.contains(keyword)) {
					return i;
				}
			}
		}
		return -1;
	}

	private boolean isNoise(String line) {
		String lower = line.toLowerCase(Locale.ROOT);
		if (lower.contains("tiktok") || lower.contains("j&t") || lower.contains("drop-off")
				|| lower.contains("drop off") || lower.contains("tracking") || lower.contains("order id")
				|| lower.contains("order 10") || lower.contains("sku") || lower.contains("qty")
				|| lower.contains("free shipping") || lower.contains("barcode")
				|| lower.contains("self-collection") || lower.contains("in transit")
				|| lower.contains("created") || lower.contains("product name")
				|| lower.contains("hydroponic") || lower.contains("fullset")) {
			return true;
		}
		if (line.length() <= 2) {
			return true;
		}
		if (TRACKING.matcher(line.replace(" ", "")).matches() && !POSTCODE.matcher(line).find()) {
			return true;
		}
		return false;
	}

	private boolean looksLikePersonOrShopName(String line) {
		if (POSTCODE.matcher(line).find()) {
			return false;
		}
		if (line.matches("(?i).*\\b(jalan|jln|lorong|kg|kampung|taman|seksyen|no\\.?\\s*\\d).*")) {
			return false;
		}
		return line.matches(".*[A-Za-z].*") && line.length() <= 60;
	}

	private boolean looksLikeAddress(String line) {
		String lower = line.toLowerCase(Locale.ROOT);
		return lower.contains("jalan") || lower.contains("jln") || lower.contains("lorong")
				|| lower.contains("kg ") || lower.contains("kampung") || lower.contains("taman")
				|| lower.contains("seksyen") || lower.contains("no ") || lower.startsWith("no.")
				|| lower.contains("selangor") || lower.contains("wilayah") || lower.contains("kuala")
				|| lower.contains("petaling") || lower.contains("shah alam") || lower.contains("klang")
				|| POSTCODE.matcher(line).find();
	}

	private String findPhone(String rawText) {
		Matcher matcher = PHONE.matcher(rawText.replace('–', '-'));
		if (matcher.find()) {
			return matcher.group(1).replaceAll("\\s+", "");
		}
		return null;
	}

	private String findBestPostcode(String rawText, List<String> lines) {
		for (String line : lines) {
			if (looksLikeAddress(line)) {
				Matcher m = POSTCODE.matcher(line);
				if (m.find()) {
					return m.group(1);
				}
			}
		}
		Matcher m = POSTCODE.matcher(rawText);
		if (m.find()) {
			return m.group(1);
		}
		return null;
	}

	private String findSortCode(String rawText) {
		Matcher matcher = SORT_CODE.matcher(rawText.replace(" ", ""));
		String fallback = null;
		while (matcher.find()) {
			String code = matcher.group(1).toUpperCase(Locale.ROOT);
			if (code.startsWith("PJ") || code.startsWith("KL") || code.startsWith("JB")
					|| code.startsWith("PG") || code.startsWith("KT") || code.startsWith("SG")) {
				return code;
			}
			fallback = code;
		}
		return fallback;
	}

	private String guessAreaKey(String address) {
		if (address == null || address.isBlank()) {
			return "UNKNOWN";
		}
		String[] parts = address.split(",");
		String last = parts[parts.length - 1].trim();
		return last.isEmpty() ? "UNKNOWN" : last.toUpperCase(Locale.ROOT);
	}

	private record ReceiverBlock(String name, String address, String postcode) {
	}
}

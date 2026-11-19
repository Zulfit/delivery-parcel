# Parcel Delivery (OCR route planner)

Free MVP: mobile web UI to take/upload parcel label photos → Tesseract OCR → geocode addresses → nearest-stop delivery order ending at hub.

Study CRUD project is separate (`../Spring-Boot`). This app lives only in `Parcel-Delivery`.

## Requirements

- Java 21
- Tesseract OCR (`brew install tesseract`) — the app calls the `tesseract` CLI (Homebrew path)

## Run (laptop)

```bash
cd /Users/zulfitrihakeem/Documents/Project/Parcel-Delivery
export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home"
./mvnw spring-boot:run
```

Or with the packaged jar:

```bash
export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home"
./mvnw -DskipTests package
java -jar target/delivery-0.0.1-SNAPSHOT.jar
```

- On laptop browser: http://localhost:8081
- Port **8081** (study CRUD app uses 8080)

## Use on your phone (same Wi‑Fi) — for tomorrow

1. Keep the laptop **on** and the app **running**.
2. Phone and laptop on the **same Wi‑Fi**.
3. Find your laptop LAN IP (Mac → System Settings → Network → Wi‑Fi → Details, or run):

```bash
ipconfig getifaddr en0
```

4. On your phone browser open:

```text
http://YOUR_LAPTOP_IP:8081
```

Example: `http://192.168.0.23:8081`

5. Tap **Take photo** or **Upload bulk** → **Plan route**.

If the phone cannot connect, allow Java/incoming connections in macOS Firewall for port 8081.

## Hub (default)

- **J&T Express-Klang Jaya (Pjs380)**
- `3.010177, 101.458588`
- https://maps.google.com/?q=3.010177,101.458588

## API (optional)

```bash
# 1) Create batch
curl -s -X POST http://localhost:8081/api/batches -H "Content-Type: application/json" -d '{}'

# 2) Upload parcel photos
curl -s -X POST "http://localhost:8081/api/batches/1/parcels" \
  -F "files=@/path/to/label1.jpg" \
  -F "files=@/path/to/label2.jpg"

# 3) Plan route
curl -s -X POST http://localhost:8081/api/batches/1/plan
```

## Notes

- Mobile UI is a web page served by Spring Boot (Flutter not required for tomorrow’s test).
- OCR is best-effort; wrinkled labels may need a quick visual check.
- Geocoding: **Plan route** looks up each address on OpenStreetMap [Nominatim](https://nominatim.org/release-docs/latest/api/Search/), so the laptop needs internet while planning. Nominatim allows 1 request/second, so planning takes about 1 second per parcel. Stops with coordinates are ordered nearest-first from the hub and get a Google Maps link; stops that cannot be located follow, grouped by postcode. Settings live under `delivery.geocoding.*` in `application.properties` (`enabled=false` turns it off and falls back to postcode grouping).
- Uploaded images are stored under `uploads/`.
- H2 data resets when the app stops.
- H2 console: http://localhost:8081/h2-console (JDBC URL `jdbc:h2:mem:parceldb`, user `sa`, empty password).

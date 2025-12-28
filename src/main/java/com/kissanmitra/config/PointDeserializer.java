package com.kissanmitra.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.geo.Point;

import java.io.IOException;

/**
 * Custom deserializer for Spring Data Point to support GeoJSON format.
 *
 * <p>Supports both GeoJSON format:
 *   { "type": "Point", "coordinates": [longitude, latitude] }
 * and standard format:
 *   { "x": longitude, "y": latitude }
 */
public class PointDeserializer extends JsonDeserializer<Point> {

    @Override
    public Point deserialize(final JsonParser jsonParser, final DeserializationContext context)
            throws IOException {
        final JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        // Handle GeoJSON format: { "type": "Point", "coordinates": [lng, lat] }
        if (node.has("type") && node.has("coordinates")) {
            final String type = node.get("type").asText();
            if ("Point".equals(type)) {
                final JsonNode coordinates = node.get("coordinates");
                if (coordinates.isArray() && coordinates.size() >= 2) {
                    final double longitude = coordinates.get(0).asDouble();
                    final double latitude = coordinates.get(1).asDouble();
                    return new Point(longitude, latitude);
                }
            }
        }

        // Handle standard format: { "x": lng, "y": lat }
        if (node.has("x") && node.has("y")) {
            final double x = node.get("x").asDouble();
            final double y = node.get("y").asDouble();
            return new Point(x, y);
        }

        // If neither format matches, return null
        return null;
    }
}


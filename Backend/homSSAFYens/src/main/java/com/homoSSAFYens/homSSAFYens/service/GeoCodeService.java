package com.homoSSAFYens.homSSAFYens.service;

import com.homoSSAFYens.homSSAFYens.client.SgisApiClient;
import com.homoSSAFYens.homSSAFYens.dto.LatLonDto;
import com.homoSSAFYens.homSSAFYens.dto.SgisGeoCodewgs84Response;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GeoCodeService {

    private final SgisApiClient apiClient;

    public Optional<LatLonDto> geocodeLatLon(String address) {
        Optional<SgisGeoCodewgs84Response> geo = apiClient.getGeoCodewgs84(address);

        if (geo.isEmpty()
                || geo.get().getResult() == null
                || geo.get().getResult().getResultData() == null
                || geo.get().getResult().getResultData().isEmpty()) {
            return Optional.empty();
        }

        var first = geo.get().getResult().getResultData().get(0);
        double lon = parseOrThrow(first.getX(), "x(lon)");
        double lat = parseOrThrow(first.getY(), "y(lat)");
        return Optional.of(new LatLonDto(lat, lon));
    }

    private static double parseOrThrow(String s, String field) {
        if (s == null || s.isBlank() || "null".equalsIgnoreCase(s)) {
            throw new IllegalStateException("SGIS geocode: " + field + " is null/blank");
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("SGIS geocode: " + field + " is not a number: " + s);
        }
    }
}

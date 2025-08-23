package com.homoSSAFYens.homSSAFYens.controller;


import com.homoSSAFYens.homSSAFYens.common.ApiResponse;
import com.homoSSAFYens.homSSAFYens.dto.LatLonDto;
import com.homoSSAFYens.homSSAFYens.service.GeoCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("api/v1/geocode")
@RequiredArgsConstructor
public class GeoCodeController {

    private final GeoCodeService geoCodeService;


    @GetMapping("/coor")
    public ApiResponse<LatLonDto> getCoordinates(@RequestParam("addr") String addr) {

        Optional<LatLonDto> res = geoCodeService.geocodeLatLon(addr);

        if (res.isEmpty()) {
            // 검색 결과 없음
            return ApiResponse.success("일치하는 주소/장소가 없습니다",null);
        }

        return ApiResponse.success("검색 성공", res.get());
    }
}

package com.homoSSAFYens.homSSAFYens.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SgisGeoCodewgs84Response {

    private Integer errCd;   // 0 이면 성공
    private String errMsg;
    private String id;
    private String trId;
    private Result result;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private String returncount;
        private String totalcount;
        private String pagenum;
        private String matching;

        // SGIS가 "resultdata" 키로 내려줌
        @JsonProperty("resultdata")
        private List<Item> resultData;
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        // 필드명은 JSON 키와 동일하게 두고, 필요 시 @JsonProperty 사용
        @JsonProperty("addr_type") private String addrType;
        @JsonProperty("road_nm") private String roadNm;
        @JsonProperty("adm_cd") private String admCd;
        @JsonProperty("road_nm_sub_no") private String roadNmSubNo;
        @JsonProperty("adm_nm") private String admNm;
        @JsonProperty("jibun_sub_no") private String jibunSubNo;
        @JsonProperty("bd_sub_nm") private String bdSubNm;
        @JsonProperty("ri_nm") private String riNm;
        @JsonProperty("sido_cd") private String sidoCd;
        @JsonProperty("sgg_nm") private String sggNm;
        @JsonProperty("sido_nm") private String sidoNm;
        @JsonProperty("sgg_cd") private String sggCd;
        @JsonProperty("road_nm_main_no") private String roadNmMainNo;
        @JsonProperty("road_cd") private String roadCd;
        @JsonProperty("leg_cd") private String legCd;
        @JsonProperty("bd_main_nm") private String bdMainNm;

        // SGIS 4326: x=경도(lon), y=위도(lat) — 문자열로 오므로 double 변환을 위해 보관
        private String x;  // lon
        private String y;  // lat

        @JsonProperty("leg_nm") private String legNm;
        @JsonProperty("ri_cd") private String riCd;
        @JsonProperty("jibun_main_no") private String jibunMainNo;
    }
}

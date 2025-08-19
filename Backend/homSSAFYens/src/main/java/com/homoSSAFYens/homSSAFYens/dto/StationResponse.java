package com.homoSSAFYens.homSSAFYens.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StationResponse {

    private Response response;

    public Response getResponse() { return response; }
    public void setResponse(Response response) { this.response = response; }

    // --- inner classes ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        private Header header;
        private Body body;

        public Header getHeader() { return header; }
        public void setHeader(Header header) { this.header = header; }

        public Body getBody() { return body; }
        public void setBody(Body body) { this.body = body; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private String resultMsg;   // "NORMAL_CODE"
        private String resultCode;  // "00" 이면 성공

        public String getResultMsg() { return resultMsg; }
        public void setResultMsg(String resultMsg) { this.resultMsg = resultMsg; }

        public String getResultCode() { return resultCode; }
        public void setResultCode(String resultCode) { this.resultCode = resultCode; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        private int totalCount;
        private List<Item> items;
        private int pageNo;
        private int numOfRows;

        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

        public List<Item> getItems() { return items; }
        public void setItems(List<Item> items) { this.items = items; }

        public int getPageNo() { return pageNo; }
        public void setPageNo(int pageNo) { this.pageNo = pageNo; }

        public int getNumOfRows() { return numOfRows; }
        public void setNumOfRows(int numOfRows) { this.numOfRows = numOfRows; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private String stationCode;
        private double tm;          // 거리(km)
        private String addr;
        private String stationName;

        public String getStationCode() { return stationCode; }
        public void setStationCode(String stationCode) { this.stationCode = stationCode; }

        public double getTm() { return tm; }
        public void setTm(double tm) { this.tm = tm; }

        public String getAddr() { return addr; }
        public void setAddr(String addr) { this.addr = addr; }

        public String getStationName() { return stationName; }
        public void setStationName(String stationName) { this.stationName = stationName; }
    }
}

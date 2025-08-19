package com.homoSSAFYens.homSSAFYens.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SgisKeyResponse {

    private Result result;
    private Integer errCd;
    private String errMsg;
    private String id;
    private String trId;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private String accessToken;
        private String accessTimeout; // 문자열로 옴

        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getAccessTimeout() { return accessTimeout; }
        public void setAccessTimeout(String accessTimeout) { this.accessTimeout = accessTimeout; }
    }

    public Result getResult() { return result; }
    public void setResult(Result result) { this.result = result; }
    public Integer getErrCd() { return errCd; }
    public void setErrCd(Integer errCd) { this.errCd = errCd; }
    public String getErrMsg() { return errMsg; }
    public void setErrMsg(String errMsg) { this.errMsg = errMsg; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTrId() { return trId; }
    public void setTrId(String trId) { this.trId = trId; }
}


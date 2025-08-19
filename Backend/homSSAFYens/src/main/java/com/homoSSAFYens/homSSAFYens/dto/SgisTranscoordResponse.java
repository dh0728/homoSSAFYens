package com.homoSSAFYens.homSSAFYens.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SgisTranscoordResponse {

    private Result result;
    private Integer errCd;   // 0 이면 성공
    private String errMsg;   // "Success"
    private String id;       // "API_0201"
    private String trId;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private double posX; // TM X
        private double posY; // TM Y

        public double getPosX() { return posX; }
        public void setPosX(double posX) { this.posX = posX; }
        public double getPosY() { return posY; }
        public void setPosY(double posY) { this.posY = posY; }
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

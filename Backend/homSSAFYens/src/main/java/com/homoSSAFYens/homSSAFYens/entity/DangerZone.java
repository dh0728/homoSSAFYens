package com.homoSSAFYens.homSSAFYens.entity;


import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "danger_zone")
public class DangerZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="ymdh", nullable=false)
    private LocalDateTime ymdh;          // 연월일시

    @Column(name="sgg_nm", length=100)
    private String sggNm;                // 시군구명

    @Column(name="spot_nm", length=100)
    private String spotNm;               // 지점명

    @Column(name="sta_nm", length=100)
    private String staNm;                // 정점명

    @Column(name="sta_lo")               // 경도
    private Double staLo;

    @Column(name="sta_la")               // 위도
    private Double staLa;

    @Column(name="tdlv_rsng_ve", precision=10, scale=3)
    private BigDecimal tdlvRsngVe;       // 조위상승속도
}

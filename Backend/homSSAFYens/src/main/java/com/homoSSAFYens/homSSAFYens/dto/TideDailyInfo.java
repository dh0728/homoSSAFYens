package com.homoSSAFYens.homSSAFYens.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record TideDailyInfo (

    LocalDate date,         // 2025-07-21
    String weekday,         // "월"
    String lunar,           // "6-27"
    String locationName,    // "부산"
    String mul,             // "4물"

    LocalTime sunrise,      // 05:51
    LocalTime sunset,       // 19:00
    LocalTime moonrise,     // 07:32
    LocalTime moonset,      // 19:59
    List<TideEvent> events // jowi1~4
) {}

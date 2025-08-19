package com.homoSSAFYens.homSSAFYens.dto;

import java.util.List;

public record AirBody (

    Integer pageNo,
    Integer numOfRows,
    Integer totalCount,
    List<AirItemDto> items
){}

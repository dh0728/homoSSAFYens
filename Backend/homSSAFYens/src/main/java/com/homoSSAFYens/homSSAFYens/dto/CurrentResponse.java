package com.homoSSAFYens.homSSAFYens.dto;

import java.util.List;

public record CurrentResponse(
        List<CurrentInfo> weather,
        CurrentCity info
) {}



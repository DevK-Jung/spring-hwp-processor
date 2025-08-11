package com.kjung.springhwpprocessor.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class HwpGenerateReq {
    private String templatePath;
    private String outputPath;
    private Map<String, ?> data;

}

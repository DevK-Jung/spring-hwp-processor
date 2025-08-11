package com.kjung.springhwpprocessor.controller;

import com.kjung.springhwpprocessor.dto.HwpGenerateReq;
import com.kjung.springhwpprocessor.service.HwpTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/hwp/generate")
public class HwpTemplateController {

    private final HwpTemplateService hwpTemplateService;

    @PostMapping
    public void generateHwpTemplate(HwpGenerateReq param) throws Exception {
        hwpTemplateService.process(param.getTemplatePath(), param.getOutputPath(), param.getData());
    }
}

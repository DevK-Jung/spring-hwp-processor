package com.kjung.springhwpprocessor.service;

import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.object.bodytext.Section;
import kr.dogfoot.hwplib.object.bodytext.control.Control;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.ControlType;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;
import kr.dogfoot.hwplib.object.bodytext.control.table.Row;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharControlChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharNormal;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharType;
import kr.dogfoot.hwplib.reader.HWPReader;
import kr.dogfoot.hwplib.writer.HWPWriter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Map;

@Service
public class HwpTemplateService {

    private static String templatePath = "src/main/resources/templates/template.hwp";

    /**
     * HWP 템플릿 파일을 읽고, 지정된 데이터를 사용하여 플레이스홀더를 치환한 후 새로운 HWP 파일로 저장합니다.
     *
     * @param templatePath HWP 템플릿 파일 경로
     * @param outputPath   출력할 HWP 파일 경로
     * @param data         플레이스홀더 치환에 사용할 데이터 맵
     * @throws Exception 처리 중 발생할 수 있는 예외
     */
    public void process(
            String templatePath,
            String outputPath, Map<String, ?> data) throws Exception {
        HWPFile hwpFile = HWPReader.fromFile(templatePath);

        for (Section section : hwpFile.getBodyText().getSectionList()) {
            Paragraph[] paragraphs = section.getParagraphs();
            if (paragraphs != null) {
                for (Paragraph para : paragraphs) {
                    applyReplacements(para, data);

                    if (para.getControlList() != null) {
                        for (Control ctrl : para.getControlList()) {
                            if (ctrl.getType() == ControlType.Table) {
                                ControlTable table = (ControlTable) ctrl;
                                processTable(table, data);
                            }
                        }
                    }
                }
            }
        }

        HWPWriter.toFile(hwpFile, outputPath);
    }

    private void processTable(ControlTable table, Map<String, ?> data) {
        ArrayList<Row> rows = table.getRowList();
        if (rows != null) {
            for (Row row : rows) {
                ArrayList<Cell> cells = row.getCellList();
                if (cells != null) {
                    for (Cell cell : cells) {
                        Paragraph[] cellParagraphs = cell.getParagraphList().getParagraphs();
                        if (cellParagraphs != null) {
                            for (Paragraph cellPara : cellParagraphs) {
                                applyReplacements(cellPara, data);
                            }
                        }
                    }
                }
            }
        }
    }

    private void applyReplacements(Paragraph para, Map<String, ?> data) {
        if (para == null || para.getText() == null) return;

        // 현재 텍스트 추출
        String text = extractText(para);
        if (text == null || text.isEmpty()) return;

        // 플레이스홀더 치환
        boolean modified = false;
        for (Map.Entry<String, ?> e : data.entrySet()) {
            String placeholder = "${" + e.getKey() + "}";
            if (text.contains(placeholder)) {
                text = text.replace(placeholder, e.getValue().toString());
                modified = true;
            }
        }

        if (modified) {
            // 기존 텍스트 클리어
            if (para.getText().getCharList() != null) {
                para.getText().getCharList().clear();
            }

            // 새로운 텍스트 추가
            String[] lines = text.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                addTextToParagraph(para, lines[i]);

                // 마지막 줄이 아니면 줄바꿈 추가
                if (i < lines.length - 1) {
                    HWPCharControlChar lineBreak = para.getText().addNewCharControlChar();
                    lineBreak.setCode((short) 10); // 줄바꿈 코드
                }
            }

            // 문단 끝 문자 추가 (필요한 경우)
            ensureParagraphEnd(para);
        }
    }

    private String extractText(Paragraph para) {
        if (para.getText() == null || para.getText().getCharList() == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (HWPChar hwpChar : para.getText().getCharList()) {
            if (hwpChar.getType() == HWPCharType.Normal) {
                sb.append((char) hwpChar.getCode());
            } else if (hwpChar.isLineBreak()) {
                sb.append("\n");
            }
            // 문단 끝 문자(13)는 무시
        }
        return sb.toString();
    }

    private void addTextToParagraph(Paragraph para, String text) {
        if (text == null || text.isEmpty()) return;

        for (char c : text.toCharArray()) {
            HWPCharNormal normalChar = para.getText().addNewNormalChar();
            normalChar.setCode((short) c);
        }
    }

    private void ensureParagraphEnd(Paragraph para) {
        // 문단 끝 문자가 없으면 추가
        ArrayList<HWPChar> charList = para.getText().getCharList();
        boolean hasParaBreak = false;

        if (charList != null && !charList.isEmpty()) {
            HWPChar lastChar = charList.get(charList.size() - 1);
            if (lastChar.isParaBreak()) {
                hasParaBreak = true;
            }
        }

        if (!hasParaBreak) {
            HWPCharNormal paraBreak = para.getText().addNewNormalChar();
            paraBreak.setCode((short) 13); // 문단 끝 코드
        }
    }
}
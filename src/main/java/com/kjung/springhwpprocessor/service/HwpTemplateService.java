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

import java.io.UnsupportedEncodingException;
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
        try {
            if (para == null || para.getText() == null || para.getText().getCharList() == null) return;

            ArrayList<HWPChar> newCharList = getNewCharList(para.getText().getCharList(), data);
            if (newCharList != null) {
                changeNewCharList(para, newCharList);
                removeLineSeg(para);
                removeCharShapeExceptFirstOne(para);
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Text encoding error during replacement", e);
        }
    }

    private ArrayList<HWPChar> getNewCharList(ArrayList<HWPChar> oldList, Map<String, ?> data) throws UnsupportedEncodingException {
        ArrayList<HWPChar> newList = new ArrayList<HWPChar>();
        ArrayList<HWPChar> listForText = new ArrayList<HWPChar>();
        boolean hasChanges = false;
        
        for (HWPChar ch : oldList) {
            if (ch.getType() == HWPCharType.Normal) {
                listForText.add(ch);
            } else {
                if (listForText.size() > 0) {
                    String text = toString(listForText);
                    listForText.clear();
                    String newText = changeText(text, data);
                    
                    if (newText != null) {
                        newList.addAll(toHWPCharList(newText));
                        hasChanges = true;
                    } else {
                        newList.addAll(toHWPCharList(text));
                    }
                }
                newList.add(ch);
            }
        }

        if (listForText.size() > 0) {
            String text = toString(listForText);
            listForText.clear();
            String newText = changeText(text, data);
            
            if (newText != null) {
                newList.addAll(toHWPCharList(newText));
                hasChanges = true;
            } else {
                newList.addAll(toHWPCharList(text));
            }
        }
        
        return hasChanges ? newList : null;
    }

    private String toString(ArrayList<HWPChar> listForText) throws UnsupportedEncodingException {
        StringBuffer sb = new StringBuffer();
        for (HWPChar ch : listForText) {
            HWPCharNormal chn = (HWPCharNormal) ch;
            sb.append((char) chn.getCode());
        }
        return sb.toString();
    }

    private String changeText(String text, Map<String, ?> data) {
        String result = text;
        boolean modified = false;
        
        for (Map.Entry<String, ?> entry : data.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            if (result.contains(placeholder)) {
                result = result.replace(placeholder, entry.getValue().toString());
                modified = true;
            }
        }
        
        return modified ? result : null;
    }

    private ArrayList<HWPChar> toHWPCharList(String text) {
        ArrayList<HWPChar> list = new ArrayList<HWPChar>();
        int count = text.length();
        for (int index = 0; index < count; index++) {
            HWPCharNormal chn = new HWPCharNormal();
            chn.setCode((short) text.codePointAt(index));
            list.add(chn);
        }
        return list;
    }

    private void changeNewCharList(Paragraph paragraph, ArrayList<HWPChar> newCharList) {
        paragraph.getText().getCharList().clear();
        for (HWPChar ch : newCharList) {
            paragraph.getText().getCharList().add(ch);
        }
        paragraph.getHeader().setCharacterCount(newCharList.size());
    }

    private void removeLineSeg(Paragraph paragraph) {
        paragraph.deleteLineSeg();
    }

    private void removeCharShapeExceptFirstOne(Paragraph paragraph) {
        int size = paragraph.getCharShape().getPositonShapeIdPairList().size();
        if (size > 1) {
            for (int index = 0; index < size - 1; index++) {
                paragraph.getCharShape().getPositonShapeIdPairList().remove(1);
            }
            paragraph.getHeader().setCharShapeCount(1);
        }
    }

}
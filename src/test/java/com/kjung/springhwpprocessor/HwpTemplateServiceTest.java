package com.kjung.springhwpprocessor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class HwpTemplateServiceTest {

    @Autowired
    private HwpTemplateService hwpTemplateService;

    @TempDir
    Path tempDir;

    private String templatePath;
    private String outputPath;

    @BeforeEach
    void setUp() throws IOException {
        // 테스트용 템플릿 파일을 임시 디렉토리로 복사
        ClassPathResource resource = new ClassPathResource("templates/template.hwp");

        if (resource.exists()) {
            Path tempTemplate = tempDir.resolve("template.hwp");
            Files.copy(resource.getInputStream(), tempTemplate, StandardCopyOption.REPLACE_EXISTING);
            templatePath = tempTemplate.toString();
        } else {
            // 템플릿 파일이 없으면 개발 환경 경로 사용
            templatePath = "src/main/resources/templates/template.hwp";
        }

        outputPath = tempDir.resolve("output.hwp").toString();
    }

    @Test
    void testProcessWithSimpleReplacements() throws Exception {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("name", "홍길동");
        data.put("date", "2024-01-15");
        data.put("company", "테스트회사");
        data.put("position", "대리");

        // 긴 내용과 개행이 포함된 항목들
        data.put("description", "이것은 매우 긴 설명입니다. " +
                "여러 문장으로 구성되어 있으며, 다양한 내용을 포함하고 있습니다. " +
                "템플릿 시스템이 긴 텍스트를 제대로 처리하는지 확인하기 위한 테스트입니다. " +
                "한글, English, 숫자 123, 특수문자 !@#$% 등이 모두 포함되어 있습니다.");

        data.put("multilineContent", "첫 번째 줄입니다.\n" +
                "두 번째 줄입니다.\n" +
                "세 번째 줄입니다.\n" +
                "\n" +
                "빈 줄 다음의 내용입니다.\n" +
                "마지막 줄입니다.");

        data.put("address", "서울특별시 강남구 테헤란로 123\n" +
                "○○빌딩 5층\n" +
                "우편번호: 12345");

        data.put("projectDetails", "프로젝트 개요:\n" +
                "1. 시스템 설계 및 구현\n" +
                "   - 데이터베이스 설계\n" +
                "   - API 개발\n" +
                "   - 프론트엔드 구현\n" +
                "2. 테스트 및 검증\n" +
                "   - 단위 테스트\n" +
                "   - 통합 테스트\n" +
                "   - 사용자 인수 테스트\n" +
                "3. 배포 및 유지보수\n" +
                "   - 서버 구성\n" +
                "   - 모니터링 설정\n" +
                "   - 정기 점검");

        data.put("longParagraph", "대한민국은 민주공화국이다. 대한민국의 주권은 국민에게 있고, " +
                "모든 권력은 국민으로부터 나온다. 모든 국민은 법 앞에 평등하다. " +
                "누구든지 성별·종교 또는 사회적 신분에 의하여 정치적·경제적·사회적·문화적 생활의 " +
                "모든 영역에 있어서 차별을 받지 아니한다. 사회적 특수계급의 제도는 인정되지 아니하며, " +
                "어떠한 형태로도 이를 창설할 수 없다. 훈장등의 영전은 이를 받은 자에게만 효력이 있고, " +
                "어떠한 특권도 이에 따르지 아니한다.");

        data.put("terms", "제1조 (목적)\n" +
                "본 약관은 회사가 제공하는 서비스의 이용과 관련하여 회사와 이용자 간의 권리, " +
                "의무 및 책임사항, 기타 필요한 사항을 규정함을 목적으로 합니다.\n" +
                "\n" +
                "제2조 (정의)\n" +
                "1. \"서비스\"란 회사가 제공하는 모든 서비스를 의미합니다.\n" +
                "2. \"이용자\"란 본 약관에 따라 회사가 제공하는 서비스를 받는 회원 및 비회원을 말합니다.\n" +
                "3. \"회원\"이란 회사에 개인정보를 제공하여 회원등록을 한 자로서, 회사의 정보를 지속적으로 제공받으며, " +
                "회사가 제공하는 서비스를 계속적으로 이용할 수 있는 자를 말합니다.");

        // When
        hwpTemplateService.process(templatePath, outputPath, data);

        // Then
        File outputFile = new File(outputPath);
        assertTrue(outputFile.exists(), "출력 파일이 생성되어야 합니다");
        assertTrue(outputFile.length() > 0, "출력 파일에 내용이 있어야 합니다");
    }

    @Test
    void testProcessWithSpecialCharacters() throws Exception {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("name", "김&이<박>");
        data.put("content", "특수문자 테스트: !@#$%^&*()");
        data.put("multiline", "첫번째 줄\n두번째 줄\n세번째 줄");

        // When
        hwpTemplateService.process(templatePath, outputPath, data);

        // Then
        File outputFile = new File(outputPath);
        assertTrue(outputFile.exists());
        assertTrue(outputFile.length() > 0);
    }

    @Test
    void testProcessWithEmptyData() throws Exception {
        // Given
        Map<String, Object> data = new HashMap<>();

        // When
        hwpTemplateService.process(templatePath, outputPath, data);

        // Then
        File outputFile = new File(outputPath);
        assertTrue(outputFile.exists(), "빈 데이터로도 파일이 생성되어야 합니다");
    }

    @Test
    void testProcessWithNullValues() throws Exception {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("name", null);
        data.put("date", "2024-01-15");

        // When & Then
        assertDoesNotThrow(() -> {
            hwpTemplateService.process(templatePath, outputPath, data);
        }, "null 값이 있어도 처리가 가능해야 합니다");
    }

    @Test
    void testProcessWithNumericValues() throws Exception {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("age", 30);
        data.put("salary", 50000000);
        data.put("rate", 3.14);

        // When
        hwpTemplateService.process(templatePath, outputPath, data);

        // Then
        File outputFile = new File(outputPath);
        assertTrue(outputFile.exists());
    }

    @Test
    void testProcessWithKoreanText() throws Exception {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("korean", "가나다라마바사아자차카타파하");
        data.put("mixed", "ABC123한글");
        data.put("sentence", "안녕하세요. 반갑습니다!");

        // When
        hwpTemplateService.process(templatePath, outputPath, data);

        // Then
        File outputFile = new File(outputPath);
        assertTrue(outputFile.exists());
    }

    @Test
    void testProcessWithLongText() throws Exception {
        // Given
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longText.append("긴 텍스트 테스트 ").append(i).append(" ");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("longText", longText.toString());

        // When
        hwpTemplateService.process(templatePath, outputPath, data);

        // Then
        File outputFile = new File(outputPath);
        assertTrue(outputFile.exists());
    }

    @Test
    void testProcessWithInvalidTemplatePath() {
        // Given
        String invalidPath = "non/existent/path.hwp";
        Map<String, Object> data = new HashMap<>();
        data.put("test", "value");

        // When & Then
        assertThrows(Exception.class, () -> {
            hwpTemplateService.process(invalidPath, outputPath, data);
        }, "존재하지 않는 템플릿 경로는 예외를 발생시켜야 합니다");
    }

    @Test
    void testProcessWithTableData() throws Exception {
        // Given - 테이블에 들어갈 데이터
        Map<String, Object> data = new HashMap<>();
        data.put("col1", "첫번째 컬럼");
        data.put("col2", "두번째 컬럼");
        data.put("col3", "세번째 컬럼");
        data.put("row1", "첫번째 행");
        data.put("row2", "두번째 행");

        // When
        hwpTemplateService.process(templatePath, outputPath, data);

        // Then
        File outputFile = new File(outputPath);
        assertTrue(outputFile.exists());
    }

    @Test
    void testProcessMultipleTimes() throws Exception {
        // Given
        Map<String, Object> data1 = new HashMap<>();
        data1.put("name", "첫번째");

        Map<String, Object> data2 = new HashMap<>();
        data2.put("name", "두번째");

        // When - 여러 번 처리
        String output1 = tempDir.resolve("output1.hwp").toString();
        String output2 = tempDir.resolve("output2.hwp").toString();

        hwpTemplateService.process(templatePath, output1, data1);
        hwpTemplateService.process(templatePath, output2, data2);

        // Then
        assertTrue(new File(output1).exists());
        assertTrue(new File(output2).exists());
    }
}

// 단위 테스트용 별도 클래스
class HwpTemplateServiceUnitTest {

    private HwpTemplateService hwpTemplateService;

    @BeforeEach
    void setUp() {
        hwpTemplateService = new HwpTemplateService();
    }

    @Test
    void testServiceCreation() {
        assertNotNull(hwpTemplateService, "서비스가 생성되어야 합니다");
    }
}
package com.dihai.infocollection.service;

import com.dihai.infocollection.dto.ImportResult;
import com.dihai.infocollection.dto.SubmissionForm;
import com.dihai.infocollection.model.CollectionKey;
import com.dihai.infocollection.model.SourceType;
import com.dihai.infocollection.model.Submission;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ExcelService {

    private static final String[] HEADERS = {
        "姓名",
        "开考时间（换算北京时间）",
        "考试地点（国家）",
        "cb账号（必须真实）",
        "科目",
        "操作类型",
        "具体考点（4小时面授必填）"
    };

    private final SubmissionService submissionService;

    public ExcelService(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @Transactional
    public ImportResult importWorkbook(CollectionKey collectionKey, MultipartFile file) throws IOException {
        ImportResult result = new ImportResult();
        DataFormatter formatter = new DataFormatter();
        Set<String> importedDedupeKeys = new HashSet<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();

            for (int rowIndex = 1; rowIndex <= lastRow; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isEmptyRow(row, formatter)) {
                    continue;
                }

                result.setTotalRows(result.getTotalRows() + 1);

                String name = text(row.getCell(0), formatter);
                if (name.startsWith("例")) {
                    continue;
                }

                try {
                    SubmissionForm form = new SubmissionForm();
                    form.setStudentName(name);
                    form.setExamTimeBeijing(examTime(row.getCell(1), formatter));
                    form.setExamLocationCountry(text(row.getCell(2), formatter));
                    form.setCbAccount(text(row.getCell(3), formatter));
                    form.setSubject(text(row.getCell(4), formatter));
                    form.setOperationType(text(row.getCell(5), formatter));
                    form.setTestCenter(text(row.getCell(6), formatter));
                    String dedupeKey = dedupeKey(form);
                    if (!dedupeKey.isBlank() && importedDedupeKeys.contains(dedupeKey)) {
                        throw new IllegalArgumentException("CB账号、姓名、科目在本次 Excel 中重复，已跳过："
                            + form.getCbAccount() + " / " + form.getStudentName() + " / " + form.getSubject());
                    }
                    Submission saved = submissionService.save(collectionKey, form, SourceType.EXCEL);
                    if (!dedupeKey.isBlank()) {
                        importedDedupeKeys.add(dedupeKey);
                    }
                    result.addImportedSubmission(saved);
                    result.setImportedRows(result.getImportedRows() + 1);
                } catch (RuntimeException ex) {
                    result.addError("第 " + (rowIndex + 1) + " 行：" + ex.getMessage());
                }
            }
        }

        return result;
    }

    public byte[] exportWorkbook(CollectionKey collectionKey, List<Submission> submissions) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("2026 AP");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                headerRow.createCell(i).setCellValue(HEADERS[i]);
            }

            for (int i = 0; i < submissions.size(); i++) {
                Submission submission = submissions.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(submission.getStudentName());
                row.createCell(1).setCellValue(submissionService.formatExamTime(submission.getExamTimeBeijing()));
                row.createCell(2).setCellValue(submission.getExamLocationCountry());
                row.createCell(3).setCellValue(submission.getCbAccount());
                row.createCell(4).setCellValue(submission.getSubject());
                row.createCell(5).setCellValue(submission.getOperationType());
                row.createCell(6).setCellValue(submission.getTestCenter() == null ? "" : submission.getTestCenter());
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    public byte[] templateWorkbook() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("2026 AP");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                headerRow.createCell(i).setCellValue(HEADERS[i]);
            }

            Row exampleOne = sheet.createRow(1);
            exampleOne.createCell(0).setCellValue("例：王五");
            exampleOne.createCell(1).setCellValue("2026/05/04 08:00 (GMT+8)");
            exampleOne.createCell(2).setCellValue("香港");
            exampleOne.createCell(3).setCellValue("12345@xxx.com");
            exampleOne.createCell(4).setCellValue("Biology");
            exampleOne.createCell(5).setCellValue("4小时线下面授/2小时网面全题/基础水印（选择题+大题提示）/混合水印（2小时网面大题）/高级水印（保5/仅限境外）");
            exampleOne.createCell(6).setCellValue("如操作类型为4小时面授，必须填写具体考点");

            Row exampleTwo = sheet.createRow(2);
            exampleTwo.createCell(0).setCellValue("例：王五");
            exampleTwo.createCell(1).setCellValue("2026/05/04 12:00 (GMT+8)");
            exampleTwo.createCell(2).setCellValue("香港");
            exampleTwo.createCell(3).setCellValue("12345@xxx.com");
            exampleTwo.createCell(4).setCellValue("European History");
            exampleTwo.createCell(5).setCellValue("2小时网面全题");
            exampleTwo.createCell(6).setCellValue("");

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private boolean isEmptyRow(Row row, DataFormatter formatter) {
        for (int i = 0; i < HEADERS.length; i++) {
            if (!text(row.getCell(i), formatter).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String examTime(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date date = cell.getDateCellValue();
            LocalDateTime time = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
            return time.toString();
        }
        return text(cell, formatter);
    }

    private String text(Cell cell, DataFormatter formatter) {
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private String dedupeKey(SubmissionForm form) {
        String cbAccount = clean(form.getCbAccount()).toLowerCase(Locale.ROOT);
        String studentName = clean(form.getStudentName()).toLowerCase(Locale.ROOT);
        String subject = clean(form.getSubject()).toLowerCase(Locale.ROOT);
        if (cbAccount.isBlank() || studentName.isBlank() || subject.isBlank()) {
            return "";
        }
        return cbAccount + "\n" + studentName + "\n" + subject;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}

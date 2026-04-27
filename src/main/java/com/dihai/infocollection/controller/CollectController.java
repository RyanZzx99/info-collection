package com.dihai.infocollection.controller;

import com.dihai.infocollection.dto.BatchSubmissionForm;
import com.dihai.infocollection.dto.NaturalLanguageForm;
import com.dihai.infocollection.dto.SubmissionForm;
import com.dihai.infocollection.model.CollectionKey;
import com.dihai.infocollection.service.CollectionKeyService;
import com.dihai.infocollection.service.ExcelService;
import com.dihai.infocollection.service.FormOptions;
import com.dihai.infocollection.service.NaturalLanguageRecognitionService;
import com.dihai.infocollection.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
public class CollectController {

    private final CollectionKeyService collectionKeyService;
    private final ExcelService excelService;
    private final NaturalLanguageRecognitionService naturalLanguageRecognitionService;
    private final SubmissionService submissionService;

    public CollectController(
        CollectionKeyService collectionKeyService,
        ExcelService excelService,
        NaturalLanguageRecognitionService naturalLanguageRecognitionService,
        SubmissionService submissionService
    ) {
        this.collectionKeyService = collectionKeyService;
        this.excelService = excelService;
        this.naturalLanguageRecognitionService = naturalLanguageRecognitionService;
        this.submissionService = submissionService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/collect")
    public String collectIndex(Model model) {
        List<CollectionKey> enabledKeys = collectionKeyService.findAll().stream()
            .filter(CollectionKey::isEnabled)
            .toList();
        model.addAttribute("keys", enabledKeys);
        return "collect-index";
    }

    @GetMapping("/collect/{key}")
    public String form(@PathVariable String key, Model model) {
        prepareCollectModel(key, model, BatchSubmissionForm.withBlankRow(), new NaturalLanguageForm());
        return "collect";
    }

    @PostMapping("/collect/{key}/parse")
    public String parse(
        @PathVariable String key,
        @Valid @ModelAttribute("naturalLanguageForm") NaturalLanguageForm naturalLanguageForm,
        BindingResult bindingResult,
        Model model
    ) {
        BatchSubmissionForm batchSubmissionForm = bindingResult.hasErrors()
            ? BatchSubmissionForm.withBlankRow()
            : BatchSubmissionForm.of(naturalLanguageRecognitionService.recognizeBatch(naturalLanguageForm.getRawText()));
        prepareCollectModel(key, model, batchSubmissionForm, naturalLanguageForm);
        model.addAttribute("parsed", !bindingResult.hasErrors());
        model.addAttribute("parsedCount", batchSubmissionForm.getRows().size());
        return "collect";
    }

    @PostMapping("/collect/{key}")
    public String submit(
        @PathVariable String key,
        @Valid @ModelAttribute("batchSubmissionForm") BatchSubmissionForm batchSubmissionForm,
        BindingResult bindingResult,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            prepareCollectModel(key, model, batchSubmissionForm, new NaturalLanguageForm());
            model.addAttribute("formError", "提交失败：请检查表格中的红色提示");
            return "collect";
        }

        try {
            CollectionKey collectionKey = collectionKeyService.getEnabledByKey(key);
            var saveResult = submissionService.saveAll(collectionKey, batchSubmissionForm.getRows());
            if (saveResult.hasDuplicates()) {
                BatchSubmissionForm duplicateForm = BatchSubmissionForm.of(saveResult.getDuplicateForms());
                prepareCollectModel(key, model, duplicateForm, new NaturalLanguageForm());
                model.addAttribute("submittedCount", saveResult.getSavedCount());
                model.addAttribute("duplicateSubmissions", saveResult.getDuplicateSubmissions());
                return "collect";
            }

            model.addAttribute("collectionKey", collectionKey);
            model.addAttribute("submittedCount", saveResult.getSavedCount());
            return "collect-success";
        } catch (IllegalArgumentException ex) {
            prepareCollectModel(key, model, batchSubmissionForm, new NaturalLanguageForm());
            model.addAttribute("formError", "提交失败：" + ex.getMessage());
            return "collect";
        }
    }

    @GetMapping("/collect/{key}/template")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable String key) throws IOException {
        collectionKeyService.getEnabledByKey(key);
        byte[] bytes = excelService.templateWorkbook();
        String filename = key + "-template.xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
            .filename(filename, StandardCharsets.UTF_8)
            .build());

        return ResponseEntity.ok()
            .headers(headers)
            .body(bytes);
    }

    @PostMapping("/collect/{key}/import")
    public String importExcel(
        @PathVariable String key,
        @RequestParam("file") MultipartFile file,
        Model model
    ) {
        try {
            CollectionKey collectionKey = collectionKeyService.getEnabledByKey(key);
            var importResult = excelService.importWorkbook(collectionKey, file);
            prepareCollectModel(key, model, BatchSubmissionForm.withBlankRow(), new NaturalLanguageForm());
            model.addAttribute("importResult", importResult);
            return "collect";
        } catch (IOException | IllegalArgumentException ex) {
            prepareCollectModel(key, model, BatchSubmissionForm.withBlankRow(), new NaturalLanguageForm());
            model.addAttribute("formError", "Excel 导入失败：" + ex.getMessage());
            return "collect";
        }
    }

    private void prepareCollectModel(
        String key,
        Model model,
        BatchSubmissionForm batchSubmissionForm,
        NaturalLanguageForm naturalLanguageForm
    ) {
        CollectionKey collectionKey = collectionKeyService.getEnabledByKey(key);
        model.addAttribute("collectionKey", collectionKey);
        model.addAttribute("batchSubmissionForm", batchSubmissionForm);
        model.addAttribute("naturalLanguageForm", naturalLanguageForm);
        model.addAttribute("locations", FormOptions.EXAM_LOCATIONS);
        model.addAttribute("subjects", FormOptions.AP_SUBJECTS);
        model.addAttribute("operationTypes", FormOptions.OPERATION_TYPES);
        model.addAttribute("submissionService", submissionService);
    }
}

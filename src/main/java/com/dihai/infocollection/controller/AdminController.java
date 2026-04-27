package com.dihai.infocollection.controller;

import com.dihai.infocollection.dto.AnnouncementForm;
import com.dihai.infocollection.dto.CollectionKeyForm;
import com.dihai.infocollection.dto.ImportResult;
import com.dihai.infocollection.dto.SubmissionForm;
import com.dihai.infocollection.model.CollectionKey;
import com.dihai.infocollection.model.Submission;
import com.dihai.infocollection.service.CollectionKeyService;
import com.dihai.infocollection.service.ExcelService;
import com.dihai.infocollection.service.FormOptions;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final CollectionKeyService collectionKeyService;
    private final SubmissionService submissionService;
    private final ExcelService excelService;

    public AdminController(
        CollectionKeyService collectionKeyService,
        SubmissionService submissionService,
        ExcelService excelService
    ) {
        this.collectionKeyService = collectionKeyService;
        this.submissionService = submissionService;
        this.excelService = excelService;
    }

    @GetMapping
    public String index(Model model) {
        prepareAdminIndex(model, new CollectionKeyForm());
        return "admin";
    }

    @PostMapping("/keys")
    public String createKey(
        @Valid @ModelAttribute("collectionKeyForm") CollectionKeyForm form,
        BindingResult bindingResult,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            prepareAdminIndex(model, form);
            return "admin";
        }

        try {
            collectionKeyService.create(form);
            return "redirect:/admin";
        } catch (IllegalArgumentException ex) {
            prepareAdminIndex(model, form);
            model.addAttribute("keyError", ex.getMessage());
            return "admin";
        }
    }

    @PostMapping("/keys/{key}/toggle")
    public String toggleKey(@PathVariable String key) {
        collectionKeyService.toggle(key);
        return "redirect:/admin";
    }

    @GetMapping("/keys/{key}")
    public String detail(@PathVariable String key, Model model) {
        CollectionKey collectionKey = collectionKeyService.getByKey(key);
        prepareAdminDetail(model, collectionKey, AnnouncementForm.of(collectionKey.getAnnouncementText()));
        return "admin-detail";
    }

    @PostMapping("/keys/{key}/announcement")
    public String updateAnnouncement(
        @PathVariable String key,
        @Valid @ModelAttribute("announcementForm") AnnouncementForm announcementForm,
        BindingResult bindingResult,
        Model model
    ) {
        CollectionKey collectionKey = collectionKeyService.getByKey(key);
        if (bindingResult.hasErrors()) {
            prepareAdminDetail(model, collectionKey, announcementForm);
            model.addAttribute("announcementError", "保存失败：请检查页面说明文字");
            return "admin-detail";
        }

        collectionKeyService.updateAnnouncement(key, announcementForm.getAnnouncementText());
        collectionKey = collectionKeyService.getByKey(key);
        prepareAdminDetail(model, collectionKey, AnnouncementForm.of(collectionKey.getAnnouncementText()));
        model.addAttribute("announcementSuccess", "页面说明文字已保存");
        return "admin-detail";
    }

    @PostMapping("/keys/{key}/import")
    public String importExcel(
        @PathVariable String key,
        @RequestParam("file") MultipartFile file,
        Model model
    ) throws IOException {
        CollectionKey collectionKey = collectionKeyService.getByKey(key);
        ImportResult result = excelService.importWorkbook(collectionKey, file);
        prepareAdminDetail(model, collectionKey, AnnouncementForm.of(collectionKey.getAnnouncementText()));
        model.addAttribute("importResult", result);
        return "admin-detail";
    }

    @PostMapping("/keys/{key}/submissions/{submissionId}/delete")
    public String deleteSubmission(
        @PathVariable String key,
        @PathVariable Long submissionId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            CollectionKey collectionKey = collectionKeyService.getByKey(key);
            submissionService.deleteByCollectionKeyAndId(collectionKey, submissionId);
            redirectAttributes.addFlashAttribute("deleteSuccess", "已删除一条信息");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("deleteError", "删除失败：" + ex.getMessage());
        }
        return "redirect:/admin/keys/" + key;
    }

    @GetMapping("/keys/{key}/submissions/{submissionId}/edit")
    public String editSubmission(
        @PathVariable String key,
        @PathVariable Long submissionId,
        Model model
    ) {
        CollectionKey collectionKey = collectionKeyService.getByKey(key);
        Submission submission = submissionService.getByCollectionKeyAndId(collectionKey, submissionId);
        prepareEditSubmissionModel(model, collectionKey, submissionId, submissionService.toForm(submission));
        return "admin-edit-submission";
    }

    @PostMapping("/keys/{key}/submissions/{submissionId}/edit")
    public String updateSubmission(
        @PathVariable String key,
        @PathVariable Long submissionId,
        @Valid @ModelAttribute("submissionForm") SubmissionForm submissionForm,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        CollectionKey collectionKey = collectionKeyService.getByKey(key);
        if (bindingResult.hasErrors()) {
            prepareEditSubmissionModel(model, collectionKey, submissionId, submissionForm);
            model.addAttribute("editError", "保存失败：请检查表单中的红色提示");
            return "admin-edit-submission";
        }

        try {
            submissionService.updateByCollectionKeyAndId(collectionKey, submissionId, submissionForm);
            redirectAttributes.addFlashAttribute("updateSuccess", "已修改一条信息");
            return "redirect:/admin/keys/" + key;
        } catch (IllegalArgumentException ex) {
            prepareEditSubmissionModel(model, collectionKey, submissionId, submissionForm);
            model.addAttribute("editError", "保存失败：" + ex.getMessage());
            return "admin-edit-submission";
        }
    }

    @GetMapping("/keys/{key}/export")
    public ResponseEntity<byte[]> exportExcel(@PathVariable String key) throws IOException {
        CollectionKey collectionKey = collectionKeyService.getByKey(key);
        List<Submission> submissions = submissionService.findByCollectionKey(collectionKey);
        byte[] bytes = excelService.exportWorkbook(collectionKey, submissions);
        String filename = collectionKey.getCollectionKey() + "-submissions.xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
            .filename(filename, StandardCharsets.UTF_8)
            .build());

        return ResponseEntity.ok()
            .headers(headers)
            .body(bytes);
    }

    private void prepareAdminIndex(Model model, CollectionKeyForm form) {
        List<CollectionKey> keys = collectionKeyService.findAll();
        Map<Long, Long> counts = keys.stream()
            .collect(Collectors.toMap(CollectionKey::getId, submissionService::countByCollectionKey));
        model.addAttribute("keys", keys);
        model.addAttribute("counts", counts);
        model.addAttribute("collectionKeyForm", form);
    }

    private void prepareAdminDetail(Model model, CollectionKey collectionKey, AnnouncementForm announcementForm) {
        List<Submission> submissions = submissionService.findByCollectionKey(collectionKey);
        model.addAttribute("collectionKey", collectionKey);
        model.addAttribute("submissions", submissions);
        model.addAttribute("submissionService", submissionService);
        model.addAttribute("announcementForm", announcementForm);
    }

    private void prepareEditSubmissionModel(
        Model model,
        CollectionKey collectionKey,
        Long submissionId,
        SubmissionForm submissionForm
    ) {
        model.addAttribute("collectionKey", collectionKey);
        model.addAttribute("submissionId", submissionId);
        model.addAttribute("submissionForm", submissionForm);
        model.addAttribute("locations", FormOptions.EXAM_LOCATIONS);
        model.addAttribute("subjects", FormOptions.AP_SUBJECTS);
        model.addAttribute("operationTypes", FormOptions.OPERATION_TYPES);
    }
}

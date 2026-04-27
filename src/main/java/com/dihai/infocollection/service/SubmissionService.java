package com.dihai.infocollection.service;

import com.dihai.infocollection.dto.BatchSaveResult;
import com.dihai.infocollection.dto.DuplicateSubmissionInfo;
import com.dihai.infocollection.dto.SubmissionForm;
import com.dihai.infocollection.model.CollectionKey;
import com.dihai.infocollection.model.SourceType;
import com.dihai.infocollection.model.Submission;
import com.dihai.infocollection.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class SubmissionService {

    private static final List<DateTimeFormatter> INPUT_FORMATTERS = List.of(
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy-M-d H:mm"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy/M/d H:mm"),
        DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy.M.d H:mm")
    );

    private static final DateTimeFormatter DISPLAY_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm '(GMT+8)'", Locale.ENGLISH);
    private static final DateTimeFormatter EDIT_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final SubmissionRepository submissionRepository;

    public SubmissionService(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    public List<Submission> findByCollectionKey(CollectionKey collectionKey) {
        return submissionRepository.findByCollectionKeyOrderByCreatedAtDesc(collectionKey);
    }

    public long countByCollectionKey(CollectionKey collectionKey) {
        return submissionRepository.countByCollectionKey(collectionKey);
    }

    public Submission getByCollectionKeyAndId(CollectionKey collectionKey, Long submissionId) {
        return submissionRepository.findByIdAndCollectionKey(submissionId, collectionKey)
            .orElseThrow(() -> new IllegalArgumentException("记录不存在或不属于当前收集任务"));
    }

    public SubmissionForm toForm(Submission submission) {
        SubmissionForm form = new SubmissionForm();
        form.setStudentName(submission.getStudentName());
        form.setExamTimeBeijing(submission.getExamTimeBeijing() == null ? "" : EDIT_FORMATTER.format(submission.getExamTimeBeijing()));
        form.setExamLocationCountry(submission.getExamLocationCountry());
        form.setCbAccount(submission.getCbAccount());
        form.setSubject(submission.getSubject());
        form.setOperationType(submission.getOperationType());
        form.setTestCenter(submission.getTestCenter());
        form.setRawText(submission.getRawText());
        return form;
    }

    @Transactional
    public void deleteByCollectionKeyAndId(CollectionKey collectionKey, Long submissionId) {
        Submission submission = getByCollectionKeyAndId(collectionKey, submissionId);
        submissionRepository.delete(submission);
    }

    @Transactional
    public Submission updateByCollectionKeyAndId(CollectionKey collectionKey, Long submissionId, SubmissionForm form) {
        validate(form);
        Submission submission = getByCollectionKeyAndId(collectionKey, submissionId);
        ensureNotDuplicate(collectionKey, form, submissionId);

        submission.setStudentName(clean(form.getStudentName()));
        submission.setExamTimeBeijing(parseExamTime(form.getExamTimeBeijing()));
        submission.setExamLocationCountry(clean(form.getExamLocationCountry()));
        submission.setCbAccount(normalizeCbAccount(form.getCbAccount()));
        submission.setSubject(clean(form.getSubject()));
        submission.setOperationType(clean(form.getOperationType()));
        submission.setTestCenter(cleanToNull(form.getTestCenter()));
        submission.setRawText(cleanToNull(form.getRawText()));
        return submissionRepository.save(submission);
    }

    @Transactional
    public Submission save(CollectionKey collectionKey, SubmissionForm form, SourceType sourceType) {
        validate(form);
        ensureNotDuplicate(collectionKey, form);
        return saveWithoutDuplicateCheck(collectionKey, form, sourceType);
    }

    private Submission saveWithoutDuplicateCheck(CollectionKey collectionKey, SubmissionForm form, SourceType sourceType) {
        Submission submission = new Submission();
        submission.setCollectionKey(collectionKey);
        submission.setStudentName(clean(form.getStudentName()));
        submission.setExamTimeBeijing(parseExamTime(form.getExamTimeBeijing()));
        submission.setExamLocationCountry(clean(form.getExamLocationCountry()));
        submission.setCbAccount(normalizeCbAccount(form.getCbAccount()));
        submission.setSubject(clean(form.getSubject()));
        submission.setOperationType(clean(form.getOperationType()));
        submission.setTestCenter(cleanToNull(form.getTestCenter()));
        submission.setSourceType(sourceType);
        submission.setRawText(cleanToNull(form.getRawText()));
        return submissionRepository.save(submission);
    }

    @Transactional
    public BatchSaveResult saveAll(CollectionKey collectionKey, List<SubmissionForm> forms) {
        if (forms == null || forms.isEmpty()) {
            throw new IllegalArgumentException("至少需要填写一条信息");
        }

        BatchSaveResult result = new BatchSaveResult();
        Set<DedupeKey> seen = new HashSet<>();

        for (int index = 0; index < forms.size(); index++) {
            SubmissionForm form = forms.get(index);
            int rowNumber = index + 1;

            try {
                DedupeKey dedupeKey = dedupeKey(form);
                if (dedupeKey.isComplete() && seen.contains(dedupeKey)) {
                    result.addDuplicateSubmission(DuplicateSubmissionInfo.of(
                        rowNumber,
                        form,
                        "本次提交中已存在相同 CB账号、姓名、科目"
                    ));
                    continue;
                }

                if (existsByDedupeKey(collectionKey, form)) {
                    result.addDuplicateSubmission(DuplicateSubmissionInfo.of(
                        rowNumber,
                        form,
                        "库中已存在相同 CB账号、姓名、科目"
                    ));
                    continue;
                }

                validate(form);
                SourceType sourceType = form.getRawText() == null || form.getRawText().isBlank()
                    ? SourceType.FORM
                    : SourceType.NATURAL_LANGUAGE;
                Submission saved = saveWithoutDuplicateCheck(collectionKey, form, sourceType);
                if (dedupeKey.isComplete()) {
                    seen.add(dedupeKey);
                }
                result.addSavedSubmission(saved);
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("第 " + rowNumber + " 行：" + ex.getMessage(), ex);
            }
        }

        return result;
    }

    public void validate(SubmissionForm form) {
        if (containsOfflineOperation(form.getOperationType()) && isBlank(form.getTestCenter())) {
            throw new IllegalArgumentException("操作类型为 4小时线下面授 时，必须填写具体考点");
        }
    }

    public boolean existsByDedupeKey(CollectionKey collectionKey, SubmissionForm form) {
        DedupeKey dedupeKey = dedupeKey(form);
        return dedupeKey.isComplete()
            && submissionRepository.existsByCollectionKeyAndCbAccountIgnoreCaseAndStudentNameIgnoreCaseAndSubjectIgnoreCase(
                collectionKey,
                dedupeKey.cbAccount(),
                dedupeKey.studentName(),
                dedupeKey.subject()
            );
    }

    public boolean existsByDedupeKeyExcludingId(CollectionKey collectionKey, SubmissionForm form, Long excludedId) {
        DedupeKey dedupeKey = dedupeKey(form);
        return dedupeKey.isComplete()
            && submissionRepository.existsByCollectionKeyAndCbAccountIgnoreCaseAndStudentNameIgnoreCaseAndSubjectIgnoreCaseAndIdNot(
                collectionKey,
                dedupeKey.cbAccount(),
                dedupeKey.studentName(),
                dedupeKey.subject(),
                excludedId
            );
    }

    public LocalDateTime parseExamTime(String value) {
        String normalized = clean(value)
            .replace("（GMT+8）", "")
            .replace("(GMT+8)", "")
            .replace("北京时间", "")
            .replace("T", " ")
            .replace("点", ":")
            .replace("时", ":")
            .replace("：", ":")
            .trim();

        if (normalized.matches("^\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}\\s+\\d{1,2}:$")) {
            normalized = normalized + "00";
        }

        for (DateTimeFormatter formatter : INPUT_FORMATTERS) {
            try {
                return LocalDateTime.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next accepted template.
            }
        }

        throw new IllegalArgumentException("开考时间格式无法识别：" + value);
    }

    public String formatExamTime(LocalDateTime value) {
        return value == null ? "" : DISPLAY_FORMATTER.format(value);
    }

    private boolean containsOfflineOperation(String operationType) {
        return operationType != null && operationType.contains("4小时");
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanToNull(String value) {
        String cleaned = clean(value);
        return cleaned.isEmpty() ? null : cleaned;
    }

    private String normalizeCbAccount(String value) {
        return clean(value).toLowerCase(Locale.ROOT);
    }

    private void ensureNotDuplicate(CollectionKey collectionKey, SubmissionForm form) {
        if (existsByDedupeKey(collectionKey, form)) {
            throw new IllegalArgumentException("信息已存在，不能重复录入："
                + clean(form.getCbAccount()) + " / "
                + clean(form.getStudentName()) + " / "
                + clean(form.getSubject()));
        }
    }

    private void ensureNotDuplicate(CollectionKey collectionKey, SubmissionForm form, Long excludedId) {
        if (existsByDedupeKeyExcludingId(collectionKey, form, excludedId)) {
            throw new IllegalArgumentException("已存在相同 CB账号、姓名、科目的其他记录："
                + clean(form.getCbAccount()) + " / "
                + clean(form.getStudentName()) + " / "
                + clean(form.getSubject()));
        }
    }

    private DedupeKey dedupeKey(SubmissionForm form) {
        return new DedupeKey(
            normalizeCbAccount(form.getCbAccount()),
            clean(form.getStudentName()).toLowerCase(Locale.ROOT),
            clean(form.getSubject()).toLowerCase(Locale.ROOT)
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record DedupeKey(String cbAccount, String studentName, String subject) {

        boolean isComplete() {
            return !cbAccount.isBlank() && !studentName.isBlank() && !subject.isBlank();
        }
    }
}

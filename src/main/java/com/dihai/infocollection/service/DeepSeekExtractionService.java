package com.dihai.infocollection.service;

import com.dihai.infocollection.dto.SubmissionForm;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DeepSeekExtractionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeepSeekExtractionService.class);

    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final boolean enabled;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private static final Map<String, String> LOCATION_ALIASES = Map.ofEntries(
        Map.entry("hong kong", "香港"),
        Map.entry("hk", "香港"),
        Map.entry("香港", "香港"),
        Map.entry("macau", "澳门"),
        Map.entry("macao", "澳门"),
        Map.entry("澳门", "澳门"),
        Map.entry("singapore", "新加坡"),
        Map.entry("新加坡", "新加坡"),
        Map.entry("japan", "日本"),
        Map.entry("日本", "日本"),
        Map.entry("korea", "韩国"),
        Map.entry("south korea", "韩国"),
        Map.entry("韩国", "韩国"),
        Map.entry("united states", "美国"),
        Map.entry("usa", "美国"),
        Map.entry("us", "美国"),
        Map.entry("美国", "美国"),
        Map.entry("canada", "加拿大"),
        Map.entry("加拿大", "加拿大"),
        Map.entry("uk", "英国"),
        Map.entry("united kingdom", "英国"),
        Map.entry("britain", "英国"),
        Map.entry("英国", "英国"),
        Map.entry("australia", "澳大利亚"),
        Map.entry("澳大利亚", "澳大利亚")
    );

    public DeepSeekExtractionService(
        @Value("${deepseek.api-key:}") String apiKey,
        @Value("${deepseek.api-url:https://api.deepseek.com/chat/completions}") String apiUrl,
        @Value("${deepseek.model:deepseek-v4-flash}") String model,
        @Value("${deepseek.enabled:true}") boolean enabled,
        ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.apiUrl = apiUrl;
        this.model = model;
        this.enabled = enabled;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public boolean isConfigured() {
        return enabled && !apiKey.isBlank();
    }

    public List<SubmissionForm> extractBatch(String rawText) {
        if (!isConfigured()) {
            LOGGER.info("DeepSeek extraction skipped: not configured");
            return List.of();
        }

        long startNanos = System.nanoTime();
        int textLength = rawText == null ? 0 : rawText.length();
        LOGGER.info("DeepSeek extraction started: model={}, textLength={}", model, textLength);

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody(rawText)))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOGGER.info(
                    "DeepSeek extraction completed: success=false, status={}, durationMs={}",
                    response.statusCode(),
                    elapsedMs(startNanos)
                );
                LOGGER.warn("DeepSeek extraction failed with status {}: {}", response.statusCode(), response.body());
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            LOGGER.info("DeepSeek extraction JSON: {}", stripCodeFence(content));
            List<SubmissionForm> rows = parseContent(content, rawText);
            LOGGER.info(
                "DeepSeek extraction completed: success=true, status={}, rows={}, durationMs={}",
                response.statusCode(),
                rows.size(),
                elapsedMs(startNanos)
            );
            return rows;
        } catch (IOException ex) {
            LOGGER.info("DeepSeek extraction completed: success=false, reason=io_error, durationMs={}", elapsedMs(startNanos));
            LOGGER.warn("DeepSeek extraction response parsing failed", ex);
            return List.of();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.info("DeepSeek extraction completed: success=false, reason=interrupted, durationMs={}", elapsedMs(startNanos));
            LOGGER.warn("DeepSeek extraction interrupted", ex);
            return List.of();
        } catch (RuntimeException ex) {
            LOGGER.info("DeepSeek extraction completed: success=false, reason=runtime_error, durationMs={}", elapsedMs(startNanos));
            LOGGER.warn("DeepSeek extraction failed", ex);
            return List.of();
        }
    }

    private String requestBody(String rawText) throws IOException {
        var request = objectMapper.createObjectNode();
        request.put("model", model);
        request.put("temperature", 0);
        request.set("response_format", objectMapper.createObjectNode().put("type", "json_object"));

        var messages = objectMapper.createArrayNode();
        messages.add(objectMapper.createObjectNode()
            .put("role", "system")
            .put("content", systemPrompt()));
        messages.add(objectMapper.createObjectNode()
            .put("role", "user")
            .put("content", rawText == null ? "" : rawText));
        request.set("messages", messages);

        return objectMapper.writeValueAsString(request);
    }

    private String systemPrompt() {
        return """
            你是 AP 考试信息结构化助手。请从用户文本中抽取一条或多条记录。
            多条记录通常用英文分号 ; 或中文分号 ；分隔，也可能换行分隔。
            必须只输出 JSON，不要输出解释，不要使用 Markdown。
            JSON 格式必须为：
            {
              "rows": [
                {
                  "studentName": "",
                  "examTimeBeijing": "",
                  "examLocationCountry": "",
                  "cbAccount": "",
                  "subject": "",
                  "operationType": "",
                  "testCenter": "",
                  "rawText": ""
                }
              ]
            }
            规则：
            - examTimeBeijing 使用 yyyy-MM-dd'T'HH:mm；如果没有年份，默认使用 2026 年；时间按北京时间理解。
            - examLocationCountry 必须输出中文国家/地区名，例如 香港、澳门、新加坡、日本、韩国、美国、加拿大、英国、澳大利亚；不要输出英文。
            - subject 必须尽量转换为 AP 英文科目名，例如 英语 -> English Language and Composition，英语文学 -> English Literature and Composition，生物 -> Biology，欧洲历史 -> European History。
            - operationType 只能使用以下之一，不能确定则留空：%s。
            - 如果字段无法识别，填空字符串。
            - rawText 填该条记录对应的原始片段。
            - 保持 rows 顺序与原文本一致。
            AP 科目参考列表：%s。
            """.formatted(String.join(" / ", FormOptions.OPERATION_TYPES), String.join(" / ", FormOptions.AP_SUBJECTS));
    }

    private List<SubmissionForm> parseContent(String content, String fallbackRawText) throws IOException {
        String json = stripCodeFence(content);
        JsonNode root = objectMapper.readTree(json);
        JsonNode rowsNode = root.path("rows");
        if (!rowsNode.isArray()) {
            return List.of();
        }

        List<SubmissionForm> rows = new ArrayList<>();
        for (JsonNode node : rowsNode) {
            SubmissionForm form = new SubmissionForm();
            form.setStudentName(text(node, "studentName"));
            form.setExamTimeBeijing(text(node, "examTimeBeijing"));
            form.setExamLocationCountry(normalizeLocation(text(node, "examLocationCountry")));
            form.setCbAccount(text(node, "cbAccount"));
            form.setSubject(text(node, "subject"));
            form.setOperationType(text(node, "operationType"));
            form.setTestCenter(text(node, "testCenter"));
            String rawText = text(node, "rawText");
            form.setRawText(rawText.isBlank() ? fallbackRawText : rawText);
            rows.add(form);
        }
        return rows;
    }

    private String stripCodeFence(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed;
    }

    private String text(JsonNode node, String fieldName) {
        return node.path(fieldName).asText("").trim();
    }

    private String normalizeLocation(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            return "";
        }
        String alias = normalized.toLowerCase(Locale.ROOT);
        return LOCATION_ALIASES.getOrDefault(alias, normalized);
    }

    private long elapsedMs(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }
}

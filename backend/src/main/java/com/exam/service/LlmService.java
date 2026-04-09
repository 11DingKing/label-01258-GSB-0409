package com.exam.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.exam.common.BusinessException;
import com.exam.entity.LlmConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class LlmService {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public String chat(LlmConfig config, String systemPrompt, String userPrompt) {
        String url = config.getBaseUrl();
        // 验证并处理 URL
        if (url == null || url.trim().isEmpty() || !isValidUrl(url)) {
            url = getDefaultBaseUrl(config.getProvider());
        }
        url = url.trim();
        
        // 移除末尾的斜杠
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        
        // 如果 URL 不包含 /v1，自动添加（兼容第三方代理）
        // 注意：有些 URL 如通义千问已经包含 /v1，不需要再添加
        if (!url.contains("/v1")) {
            url += "/v1";
        }
        
        // 确保 URL 以 /chat/completions 结尾
        if (!url.endsWith("/chat/completions")) {
            url += "/chat/completions";
        }

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", config.getModelName());
        requestBody.put("temperature", config.getTemperature());
        requestBody.put("max_tokens", config.getMaxTokens());

        JSONArray messages = new JSONArray();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);
        }
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.add(userMsg);
        requestBody.put("messages", messages);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + config.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody.toJSONString(), MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                log.error("LLM API error: {} - {}", response.code(), errorBody);
                
                // 提供更友好的错误提示
                String errorMessage = switch (response.code()) {
                    case 401 -> "API Key 无效或已过期，请检查大模型配置中的 API Key";
                    case 403 -> "API 访问被拒绝，请检查 API Key 权限";
                    case 404 -> "API 地址或模型名称错误，请检查配置";
                    case 429 -> "API 请求过于频繁，请稍后重试";
                    case 500, 502, 503 -> "大模型服务暂时不可用，请稍后重试";
                    default -> "大模型 API 调用失败 (错误码: " + response.code() + ")";
                };
                throw new BusinessException(errorMessage);
            }

            String responseBody = response.body().string();
            JSONObject jsonResponse = JSON.parseObject(responseBody);
            JSONArray choices = jsonResponse.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                return choices.getJSONObject(0).getJSONObject("message").getString("content");
            }
            throw new BusinessException("大模型返回空响应，请稍后重试");
        } catch (IOException e) {
            log.error("LLM API call failed", e);
            throw new BusinessException("网络连接失败，请检查 API 地址是否正确: " + e.getMessage());
        }
    }

    public String testConnection(LlmConfig config) {
        // 模拟模式直接返回成功
        if ("MOCK".equalsIgnoreCase(config.getProvider())) {
            return "✅ 连接成功（模拟模式）: 系统将使用模拟数据进行出题和批卷";
        }
        
        // 检查 API Key 是否有效
        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.length() < 10 || "mock-api-key".equals(apiKey)) {
            throw new BusinessException("API Key 未填写或无效，请检查配置");
        }
        
        try {
            String response = chat(config, null, "Say 'Connection successful!' in exactly 3 words.");
            return "✅ 连接成功: " + response;
        } catch (Exception e) {
            log.warn("API 连接测试失败: {}", e.getMessage());
            throw new BusinessException("连接失败: " + e.getMessage());
        }
    }

    private String getDefaultBaseUrl(String provider) {
        return switch (provider.toUpperCase()) {
            case "OPENAI" -> "https://api.openai.com/v1";
            case "AZURE" -> "https://api.openai.azure.com/v1";
            case "DEEPSEEK" -> "https://api.deepseek.com/v1";
            case "QWEN" -> "https://dashscope.aliyuncs.com/compatible-mode/v1";
            case "ZHIPU" -> "https://open.bigmodel.cn/api/paas/v4";
            case "DMXAPI" -> "https://www.dmxapi.cn/v1";
            default -> "https://api.openai.com/v1";
        };
    }

    /**
     * 验证 URL 是否有效（必须以 http:// 或 https:// 开头）
     */
    private boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        String trimmed = url.trim().toLowerCase();
        return trimmed.startsWith("http://") || trimmed.startsWith("https://");
    }
}

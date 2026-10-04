package com.lin.elm.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 智能客服接口
 * 调用 DeepSeek 的对话补全接口，API Key 在 application.yml 的 deepseek.api-key 中配置
 */
@RestController
@RequestMapping("ChatController")
public class ChatController {

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.base-url}")
    private String baseUrl;

    @Value("${deepseek.model}")
    private String model;

    /**
     * 前端传入 JSON：{ "message": "用户问题", "history": [{"role":"user","content":"..."}, ...] }
     * 返回 JSON：{ "success": true, "reply": "AI 的回答" }
     */
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            String message = String.valueOf(request.getOrDefault("message", ""));
            List<Map<String, Object>> history =
                    (List<Map<String, Object>>) request.getOrDefault("history", new ArrayList<>());

            // 组装 messages：系统提示词 + 历史记录 + 当前问题
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> system = new HashMap<>();
            system.put("role", "system");
            system.put("content", "你是饿了么外卖平台的智能客服，请用简洁、友好、专业的中文回答用户的问题。");
            messages.add(system);
            if (history != null) {
                messages.addAll(history);
            }
            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", message);
            messages.add(userMsg);

            // DeepSeek 对话补全请求体
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", messages);
            body.put("stream", false);

            // 请求头：Content-Type: application/json，Authorization: Bearer API Key
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/chat/completions",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map data = response.getBody();
            if (data != null && data.get("choices") instanceof List) {
                List choices = (List) data.get("choices");
                if (!choices.isEmpty()) {
                    Map choice = (Map) choices.get(0);
                    Map msg = (Map) choice.get("message");
                    Object content = msg.get("content");
                    result.put("success", true);
                    result.put("reply", content == null ? "" : content.toString());
                }
            }
            if (!result.containsKey("success")) {
                result.put("success", false);
                result.put("reply", "抱歉，AI客服暂时无法回答，请稍后再试。");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("reply", "AI客服连接失败，请检查后端 deepseek.api-key 配置是否正确。");
        }
        return result;
    }
}

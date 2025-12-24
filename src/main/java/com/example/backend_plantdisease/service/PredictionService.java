package com.example.backend_plantdisease.service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;

@Service
public class PredictionService {

    @Value("${ml.api.url}")
    private String mlApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> predict(MultipartFile file) throws Exception {
        // 1. Convert MultipartFile to Resource
        byte[] fileBytes = file.getBytes();
        ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        // 2. Set headers and build body for Hugging Face
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        
        try {
            // 3. Get Prediction from Hugging Face
            ResponseEntity<Map> response = restTemplate.postForEntity(mlApiUrl, request, Map.class);
            Map<String, Object> result = response.getBody();

            // 4. If a disease is found, get AI advice from Gemini
            if (result != null && result.containsKey("disease")) {
                String diseaseName = (String) result.get("disease");
                String aiAdvice = getAiAdvice(diseaseName); // Calling our new AI helper
                
                // Create a mutable copy and add the advice
                Map<String, Object> finalResponse = new HashMap<>(result);
                finalResponse.put("aiAdvice", aiAdvice);
                return finalResponse;
            }
            
            return result;
        } catch (Exception e) {
            System.err.println("Error calling ML API: " + e.getMessage());
            throw e;
        }
    }

    private String getAiAdvice(String diseaseName) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey;

            // Simple prompt for the farmer
            String prompt = "Act as an expert plant pathologist. The plant is diagnosed with " + diseaseName + 
                            ". Give 3 quick organic treatment steps and 1 prevention tip. Max 80 words.";

            Map<String, Object> contents = Map.of("parts", List.of(Map.of("text", prompt)));
            Map<String, Object> requestBody = Map.of("contents", List.of(contents));

            // Call Gemini
            Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);

            // Extract the text from the response structure
            List candidates = (List) response.get("candidates");
            Map firstCandidate = (Map) candidates.get(0);
            Map content = (Map) firstCandidate.get("content");
            List parts = (List) content.get("parts");
            Map firstPart = (Map) parts.get(0);
            
            return (String) firstPart.get("text");
        } catch (Exception e) {
            return "Unable to load AI advice. Please check your internet connection or try again later.";
        }
    }
}
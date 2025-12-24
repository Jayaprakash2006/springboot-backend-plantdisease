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
        // 1. Prepare the image for Hugging Face
        byte[] fileBytes = file.getBytes();
        ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        
        try {
            // 2. Step 1: Call Detection Model (Hugging Face)
            ResponseEntity<Map> response = restTemplate.postForEntity(mlApiUrl, request, Map.class);
            Map<String, Object> mlResult = response.getBody();

            // 3. Step 2: Call Generative AI (Gemini) if detection was successful
            if (mlResult != null && mlResult.containsKey("disease")) {
                String diseaseName = (String) mlResult.get("disease");
                String aiAdvice = getAiAdvice(diseaseName);
                
                // Create a NEW HashMap because mlResult might be immutable
                Map<String, Object> finalResponse = new HashMap<>(mlResult);
                finalResponse.put("aiAdvice", aiAdvice);
                return finalResponse;
            }
            
            return mlResult;
        } catch (Exception e) {
            System.err.println("Error in Prediction Flow: " + e.getMessage());
            throw e;
        }
    }

    private String getAiAdvice(String diseaseName) {
        try {
            // Updated URL to version 1.5-flash for faster response
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;

            // Define the AI Prompt
            String promptText = "Act as a professional plant pathologist. The crop has " + diseaseName + 
                                ". Provide 3 quick treatment steps and 1 prevention tip. Max 80 words.";

            // Format the JSON exactly as Gemini expects it
            Map<String, Object> parts = Map.of("text", promptText);
            Map<String, Object> contents = Map.of("parts", List.of(parts));
            Map<String, Object> requestBody = Map.of("contents", List.of(contents));

            // Execute POST request
            Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);

            // Navigate the nested JSON: candidates[0] -> content -> parts[0] -> text
            List candidates = (List) response.get("candidates");
            Map firstCandidate = (Map) candidates.get(0);
            Map content = (Map) firstCandidate.get("content");
            List resParts = (List) content.get("parts");
            Map firstPart = (Map) resParts.get(0);
            
            return ((String) firstPart.get("text")).replace("*", "");

        } catch (Exception e) {
            e.printStackTrace();
            return "AI advice currently unavailable. Consult a local expert for " + diseaseName + ".";
        }
    }

}
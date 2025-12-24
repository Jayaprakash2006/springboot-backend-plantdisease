package com.example.backend_plantdisease.service;

import java.util.Map;

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

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> predict(MultipartFile file) throws Exception {
        // 1. Convert MultipartFile to a Resource that RestTemplate understands well
        byte[] fileBytes = file.getBytes();
        ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename(); // Flask needs this!
            }
        };

        // 2. Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // 3. Build the body
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        // 4. Send the request
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(mlApiUrl, request, Map.class);
            return response.getBody();
        } catch (Exception e) {
            // Log the actual error from Hugging Face
            System.err.println("Error calling ML API: " + e.getMessage());
            throw e;
        }
    }
}
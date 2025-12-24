package com.example.backend_plantdisease.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import com.example.backend_plantdisease.service.PredictionService;
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class PredictionController {

    @Autowired
    private PredictionService predictionService;

    @PostMapping("/predict")
public ResponseEntity<?> predict(@RequestParam("file") MultipartFile file) {
    try {
        return ResponseEntity.ok(predictionService.predict(file));
    } catch (Exception e) {
        //e.printStackTrace();
        return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
    }
}

}

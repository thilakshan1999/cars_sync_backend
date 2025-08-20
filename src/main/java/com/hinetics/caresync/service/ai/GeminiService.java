package com.hinetics.caresync.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hinetics.caresync.dto.extracted.DocumentExtractedDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {
    @Value("${gemini.api-key}")
    private String apiKey;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro-002:generateContent";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper;

    public GeminiService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public DocumentExtractedDto generateText(String userPrompt) throws Exception {
        Map<String, Object> request = buildPromptRequest(userPrompt);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(request, headers);

        // Send request
        ResponseEntity<String> response = restTemplate.postForEntity(
                GEMINI_API_URL + "?key=" + apiKey,
                httpEntity,
                String.class
        );

        // Parse response
        if (response.getStatusCode().is2xxSuccessful()) {
            JsonNode root = mapper.readTree(response.getBody());
            String rawText = root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text")
                    .asText();

            int jsonStart = rawText.indexOf('{');
            int jsonEnd = rawText.lastIndexOf('}');
            if (jsonStart >= 0 && jsonEnd >= 0 && jsonEnd > jsonStart) {
                String json = rawText.substring(jsonStart, jsonEnd + 1);
                return mapper.readValue(json, DocumentExtractedDto.class);
            }

            throw new RuntimeException("Gemini response did not contain valid JSON.");
        } else {
            throw new RuntimeException("Failed to call Gemini API: " + response.getStatusCode());
        }
    }

    private static Map<String, Object> buildPromptRequest(String userPrompt) {
        String wrappedPrompt = """
                Analyze the document below and return the result in this exact JSON format:
                {
                  "documentName": "string",
                  "documentType": "string",// must be selected from the list below
                  "summary": "string",
                  "doctors": [
                     {
                     "name": "string",
                     "specialization": "string (optional)",
                     "phoneNumber": "string (optional)",
                     "email": "string (optional)",
                     "address": "string  (optional, include clinic/hospital name if mentioned)"
                     }
                  ],
                  "medicines": [
                           {
                             "medName": "string",
                             "medForm": "string" (optional), // must be selected from the list below
                             "healthCondition": "string (optional)",
                             "intakeInterval": "string (ISO-8601 duration format, e.g., 'PT6H', 'PT12H', 'P1D') (optional)",
                             "startDate": "string (ISO format: yyyy-MM-dd'T'HH:mm:ss, optional)",
                             "endDate": "string (ISO format: yyyy-MM-dd, optional)",
                             "dosage": "string (e.g., 2 pills at a time, 500ml, etc.)" (optional),
                             "instruction": "string" (optional)// must be selected from the list below
                           }
                  ],
                  "appointments": [
                      {
                        "name": "string",
                        "type": "string", // must be selected from the list below
                        "doctorName": "string",
                        "appointmentDateTime": "string (ISO format: yyyy-MM-dd'T'HH:mm:ss)" // REQUIRED
                      }
                    ],
                     "vitals": [
                            {
                              "name": "string ",
                              "unit": "string (optional)",
                              "value": "string ",
                              "dateTime": "string (ISO format: yyyy-MM-dd'T'HH:mm:ss) (optional)"
                            }
                     ]
                }
                Allowed values for documentType (choose the most relevant one):
                            - Prescription
                            - Medical Report
                            - Lab Report
                            - Discharge Summary
                            - Referral Letter
                            - Test Result
                            - Other
                Allowed values for medForm:
                        - TABLET
                        - CAPSULE
                        - SYRUP
                        - INJECTION
                        - POWDER
                        - DROPS
                        - CREAM
                        - GEL
                        - SPRAY
                        - OTHER
                Allowed values for instruction:
                        - Before Eat
                        - After Eat
                        - While Eat
                        - Doesn't Matter
                Allowed values for appointment type:
                  - CONSULTATION
                  - FOLLOW_UP
                  - SURGERY
                  - DIAGNOSIS
                  - CHECKUP
                  - EMERGENCY
                  - OTHER
                
                Notes:
                - The documentName should be a short, user-friendly label. Keep it under 50 characters.
                - If the doctor's name is not found, return an empty array for "doctors".
                - If a doctor is found but other details are missing, you may leave them as null or empty strings.
                - If a medicine name is not found, skip adding that entry to "medicines".
                - For intakeInterval, use durations like "6h", "12h", "1d", etc.
                - **Only include appointments if `appointmentDateTime` is present. Do NOT include any appointment without this field.**
                -*Always link the appointment with the doctor mentioned in the document.\s
                  If a doctor is listed in "doctors", use the same doctor's name in "appointments.doctorName".**
                - **For vitals, only include entries where both `name` and `value` are present. Otherwise, skip them.**
                Document:
                """ + userPrompt;

        // Build request body
        return Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", wrappedPrompt)
                        ))
                )
        );
    }
}

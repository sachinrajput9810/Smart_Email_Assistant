package com.email.writer.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Objects;

@Service
public class EmailGeneratorService {

    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl ;

    @Value("${gemini.api.key}")
    private String geminiApiKey ;

    public EmailGeneratorService(WebClient webClient) {
        this.webClient = webClient;
    }


    public String generateEmailReply(EmailRequest emailRequest){

        // Building the prompt
        String prompt = buildPrompt(emailRequest) ;

        // Crafting the request
        Map<String , Object> requestBody = Map.of(
                "contents" , new Object[]{
                        Map.of("parts" , new Object[]{
                            Map.of("text" , prompt),
                        })
                }
        )    ;

        // Do request and get response
        String response = webClient.post()
                .uri(geminiApiUrl +  geminiApiKey)
                .header("Content-Type" , "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // Return Extracted response
        return extractResponseContent(response)  ;
    }

    private String extractResponseContent(String response) {
        try{
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response) ;
            return rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText() ;
        }
        catch (Exception e){
            return "Error Processing request :: " + e.getMessage() ;
        }
    }

    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder() ;
        prompt.append("Dont generate the Subject for the email just Generate a professional Email reply for the following email content") ;
        if(emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()){
            prompt.append("Use a ").append(emailRequest.getTone()).append(" Tone") ;
        }
        prompt.append("\nOrignal email : \n").append(emailRequest.getEmailContent()) ;
        return prompt.toString();
    }
}

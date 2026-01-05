package com.invoice.generation.Service;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class EmailApiClient {

    @Value("${email.api.url}")
    private String emailApiUrl;

    @Value("${email.smtp.host}")
    private String smtpHost;

    @Value("${email.smtp.port}")
    private String smtpPort;

    @Value("${email.smtp.user}")
    private String smtpUser;

    @Value("${email.smtp.password}")
    private String smtpPassword;

    @Value("${email.from}")
    private String from;

    @Value("${email.cc:}")
    private String cc;

    @Value("${email.bcc:}")
    private String bcc;

    public String sendEmail(
            String filePath,
            String subject,
            String html
    ) {

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("to", cc);
        body.add("bcc",bcc);
        body.add("from", from);
        body.add("subject", subject);
        body.add("html", html);
        body.add("emailHost", smtpHost);
        body.add("emailPort", smtpPort);
        body.add("emailUser", smtpUser);
        body.add("emailPassword", smtpPassword);
        FileSystemResource filetmp = new FileSystemResource(filePath);
        body.add("file", filetmp);
        System.out.println(body);

        String response = WebClient.create()
                .post()
                .uri(emailApiUrl)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return response;

        
    }
}

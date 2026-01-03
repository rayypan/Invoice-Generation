package com.invoice.generation.Service;

import java.nio.file.Path;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class EmailApiClient {

    public static String sendEmail(
            String filePath,
            String to,
            String from,
            String subject,
            String html,
            String emailHost,
            String emailPort,
            String emailUser,
            String emailPassword
    ) {

        WebClient client = WebClient.create();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("to", to); // alice@example.com,bob@example.com
        body.add("from", from); // John Doe <johndoe@example.com>
        body.add("subject", subject);
        body.add("html", html);

        body.add("emailHost", emailHost);
        body.add("emailPort", emailPort);
        body.add("emailUser", emailUser);
        body.add("emailPassword", emailPassword);

        body.add("file", new FileSystemResource(Path.of(filePath)));

        return client.post()
                .uri("https://generic-email-service.vercel.app/api/v1/email")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}

package com.example.itqgroupttask.generator;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;

public class GeneratorMain {

    public static void main(String[] args) throws Exception {
        Path configPath = args.length > 0 ? Path.of(args[0]) : Path.of("generator-config.json");
        GeneratorConfig cfg = readConfig(configPath);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        ObjectMapper mapper = new ObjectMapper();

        System.out.println("N=" + cfg.getN() + ", baseUrl=" + cfg.getBaseUrl());
        long start = System.currentTimeMillis();
        for (int i = 1; i <= cfg.getN(); i++) {
            var body = new LinkedHashMap<String, Object>();
            body.put("initiator", cfg.getInitiator());
            body.put("author", cfg.getAuthor());
            body.put("title", cfg.getTitlePrefix() + i);

            String json = mapper.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(cfg.getBaseUrl() + "/api/documents"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                System.out.println("[" + i + "/" + cfg.getN() + "] ERROR status=" + resp.statusCode() + " body=" + resp.body());
            } else if (i == 1 || i % 50 == 0 || i == cfg.getN()) {
                System.out.println("[" + i + "/" + cfg.getN() + "] created");
            }
        }
        System.out.println("Done in " + (System.currentTimeMillis() - start) + " ms");
    }

    private static GeneratorConfig readConfig(Path path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        if (Files.exists(path)) {
            return mapper.readValue(Files.readString(path), GeneratorConfig.class);
        }
        GeneratorConfig cfg = new GeneratorConfig();
        Files.writeString(path, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(cfg));
        System.out.println("Config file not found. Created default at: " + path.toAbsolutePath());
        return cfg;
    }
}


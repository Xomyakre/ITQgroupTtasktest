package com.example.itqgroupttask;

import com.example.itqgroupttask.api.dto.BatchIdsRequest;
import com.example.itqgroupttask.api.dto.CreateDocumentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void happyPath_singleDocument_create_submit_approve_and_history() throws Exception {
        long id = create("u1", "alex", "Doc 1");

        // submit
        BatchIdsRequest submit = new BatchIdsRequest();
        submit.setInitiator("u1");
        submit.setIds(java.util.List.of(id));
        submit.setComment("submit");
        mockMvc.perform(post("/api/documents/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].result").value("SUCCESS"));

        // approve
        BatchIdsRequest approve = new BatchIdsRequest();
        approve.setInitiator("u2");
        approve.setIds(java.util.List.of(id));
        approve.setComment("approve");
        mockMvc.perform(post("/api/documents/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approve)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].result").value("SUCCESS"));

        // get with history
        var resp = mockMvc.perform(get("/api/documents/" + id).param("includeHistory", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.history.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        assertThat(resp).contains("SUBMIT").contains("APPROVE");
    }

    @Test
    void batchSubmit_partialResults() throws Exception {
        long ok = create("u1", "a", "t1");
        long missing = 9999999L;

        BatchIdsRequest submit = new BatchIdsRequest();
        submit.setInitiator("u1");
        submit.setIds(java.util.List.of(ok, missing));

        mockMvc.perform(post("/api/documents/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.length()").value(2));
    }

    @Test
    void approve_rollsBackOnRegistryError_secondApproveGetsRegistryErrorAndNoExtraHistory() throws Exception {
        long id = create("u1", "alex", "Doc 2");

        BatchIdsRequest submit = new BatchIdsRequest();
        submit.setInitiator("u1");
        submit.setIds(java.util.List.of(id));
        mockMvc.perform(post("/api/documents/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].result").value("SUCCESS"));

        BatchIdsRequest approve1 = new BatchIdsRequest();
        approve1.setInitiator("u2");
        approve1.setIds(java.util.List.of(id));
        mockMvc.perform(post("/api/documents/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approve1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].result").value("SUCCESS"));

        // повторное approve должно быть конфликтом (статус уже APPROVED)
        BatchIdsRequest approve2 = new BatchIdsRequest();
        approve2.setInitiator("u3");
        approve2.setIds(java.util.List.of(id));
        mockMvc.perform(post("/api/documents/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approve2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].result").value("CONFLICT"));
    }

    private long create(String initiator, String author, String title) throws Exception {
        CreateDocumentRequest req = new CreateDocumentRequest();
        req.setInitiator(initiator);
        req.setAuthor(author);
        req.setTitle(title);

        var json = mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(json).get("id").asLong();
    }
}


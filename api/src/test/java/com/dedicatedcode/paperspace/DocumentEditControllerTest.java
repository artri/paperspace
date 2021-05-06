package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.model.Document;
import com.dedicatedcode.paperspace.model.Page;
import com.dedicatedcode.paperspace.search.SolrService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static com.dedicatedcode.paperspace.TestHelper.randBinary;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentEditController.class)
class DocumentEditControllerTest {

    @MockBean
    private SolrService solrService;

    @MockBean
    private DocumentService documentService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/edit/{documentId}", UUID.randomUUID()))
                .andExpect(status().is(404));
    }

    @Test
    void shouldReturnEmptyPageTransformationModel() throws Exception {
        UUID documentId = UUID.randomUUID();
        Document document = new Document(documentId, LocalDateTime.now(), "Test Title", "Test Description", randBinary(),
                Arrays.asList(
                        new Page(UUID.randomUUID(), 0, "Page 1", randBinary()),
                        new Page(UUID.randomUUID(), 1, "Page 1", randBinary()),
                        new Page(UUID.randomUUID(), 2, "Page 1", randBinary())
                ), Collections.emptyList());

        when(documentService.getDocument(documentId)).thenReturn(document);

        mockMvc.perform(get("/api/edit/{documentId}", documentId))
                .andExpect(status().is(200))
                .andDo(print())
                .andExpect(jsonPath("$.length()", is(3)))
                .andExpect(jsonPath("$[0].page.id", is(document.getPages().get(0).getId().toString())))
                .andExpect(jsonPath("$[1].page.id", is(document.getPages().get(1).getId().toString())))
                .andExpect(jsonPath("$[2].page.id", is(document.getPages().get(2).getId().toString())));
    }

    @Test
    void shouldDoNothingIfNoChangeIsAvailable() throws Exception {
        UUID documentId = UUID.randomUUID();
        Document document = new Document(documentId, LocalDateTime.now(), "Test Title", "Test Description", randBinary(),
                Arrays.asList(
                        new Page(UUID.randomUUID(), 0, "Page 1", randBinary()),
                        new Page(UUID.randomUUID(), 1, "Page 2", randBinary()),
                        new Page(UUID.randomUUID(), 2, "Page 3", randBinary())
                ), Collections.emptyList());

        when(documentService.getDocument(documentId)).thenReturn(document);
        Gson gson = new GsonBuilder().create();
        this.mockMvc.perform(post("/api/edit/{documentId}", documentId)
                .content(gson.toJson(new Object())));

    }
}
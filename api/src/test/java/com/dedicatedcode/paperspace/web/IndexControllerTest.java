package com.dedicatedcode.paperspace.web;

import com.dedicatedcode.paperspace.DocumentEditController;
import com.dedicatedcode.paperspace.DocumentService;
import com.dedicatedcode.paperspace.TagService;
import com.dedicatedcode.paperspace.TestHelper;
import com.dedicatedcode.paperspace.model.Document;
import com.dedicatedcode.paperspace.model.State;
import com.dedicatedcode.paperspace.model.TaskDocument;
import com.dedicatedcode.paperspace.search.SolrService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(IndexController.class)
class IndexControllerTest {

    @MockBean
    private DocumentService documentService;
    @MockBean
    private SolrService solrService;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/task/" + UUID.randomUUID())).andExpect(status().is(404));
        mockMvc.perform(get("/document/")).andExpect(status().is(404));

        mockMvc.perform(get("/task/edit/" + UUID.randomUUID())).andExpect(status().is(404));
        mockMvc.perform(get("/document/edit/" + UUID.randomUUID())).andExpect(status().is(404));
    }

    @Test
    void shouldInjectAllTagsByName() throws Exception {
        mockMvc.perform(get("/task/" + UUID.randomUUID())).andExpect(status().is(404));
        mockMvc.perform(get("/document/")).andExpect(status().is(404));

        mockMvc.perform(get("/task/edit/" + UUID.randomUUID())).andExpect(status().is(404));
        mockMvc.perform(get("/document/edit/" + UUID.randomUUID())).andExpect(status().is(404));
    }

    @Test
    void shouldLoadTaskPage() throws Exception {
        TaskDocument task = new TaskDocument(UUID.randomUUID(), LocalDateTime.now(), "Test Task", "Description", TestHelper.randBinary(), State.OPEN, Collections.emptyList(), LocalDateTime.now().plusDays(14), null, Collections.emptyList());

        when(documentService.getDocument(task.getId())).thenReturn(task);
        ModelAndView result = mockMvc.perform(get("/task/" + task.getId()))
                .andExpect(status().is(200))
                .andExpect(model().attribute("editable", false))
                .andReturn()
                .getModelAndView();

        assertEquals(task.getId(), ((TaskDocumentResponse) result.getModel().get("document")).getId());

    }

    @Test
    void shouldLoadDocumentPage() throws Exception {
        Document document = new Document(UUID.randomUUID(), LocalDateTime.now(), "Test Task", "Description", TestHelper.randBinary(), Collections.emptyList(), Collections.emptyList());

        when(documentService.getDocument(document.getId())).thenReturn(document);
        ModelAndView result = mockMvc.perform(get("/document/" + document.getId()))
                .andExpect(status().is(200))
                .andExpect(model().attribute("editable", false))
                .andReturn()
                .getModelAndView();

        assertEquals(document.getId(), ((DocumentResponse) result.getModel().get("document")).getId());

    }
}
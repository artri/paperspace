package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.model.Binary;
import com.dedicatedcode.paperspace.model.OCRState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ErrorController.class)
class ErrorControllerTest {

    @MockBean
    private BinaryService binaryService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnEmptyDocumentOnNoErrors() throws Exception {
        mockMvc.perform(get("/api/errors.json"))
                .andDo(print())
                .andExpect(status().is(200));
    }

    @Test
    void shouldReturnDocumentsWithErrors() throws Exception {
        List<Binary> failedBinaries = Arrays.asList(
                new Binary(UUID.randomUUID(), LocalDateTime.now(), "/tmp/failed-1.pdf", "12345", "application/pdf", 1, OCRState.FAILED),
                new Binary(UUID.randomUUID(), LocalDateTime.now(), "/tmp/failed-2.pdf", "12345", "application/pdf", 1, OCRState.FAILED),
                new Binary(UUID.randomUUID(), LocalDateTime.now(), "/tmp/failed-3.pdf", "12345", "application/pdf", 1, OCRState.FAILED)
        );
        when(binaryService.getFailed()).thenReturn(failedBinaries);

        mockMvc.perform(get("/api/errors.json"))
                .andDo(print())
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("[0].path").value("/tmp/failed-1.pdf"))
                .andExpect(jsonPath("[0].links.delete").value("/api/binary/" + failedBinaries.get(0).getId() + "/delete"))
                .andExpect(jsonPath("[0].links.ignore").value("/api/binary/" + failedBinaries.get(0).getId() + "/ignore"))
                .andExpect(jsonPath("[1].path").value("/tmp/failed-2.pdf"))
                .andExpect(jsonPath("[2].path").value("/tmp/failed-3.pdf"));
    }
}
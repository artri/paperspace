package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.model.Binary;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class BinaryControllerTest {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private BinaryService binaryService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();
    }

    @Test
    void uploadBinary() throws Exception {
        MockMultipartFile file = new MockMultipartFile("data", "Test File.pdf", "application/pdf", getClass().getResourceAsStream("/test_files/A Sample PDF.pdf"));

        AtomicReference<Binary> binary = new AtomicReference<>();
        mockMvc.perform(MockMvcRequestBuilders.multipart("/binary")
                .file(file)
                .param("mimeType", "application/pdf"))
                .andExpect(status().is(201))
                .andDo(print())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.originalFileName", is("Test File.pdf")))
                .andExpect(jsonPath("$.mimeType", is("application/pdf")))
                .andExpect(jsonPath("$.length", is(9689)))
                .andDo(result -> {
                    binary.set(objectMapper.readValue(result.getResponse().getContentAsString(), Binary.class));
                });

        Binary storedBinary = this.binaryService.get(binary.get().getId());
        assertNotNull(binary);
        assertEquals("application/pdf", storedBinary.getMimeType());
        assertEquals("Test File.pdf", storedBinary.getOriginalFileName());
        assertNotNull(storedBinary.getCreatedAt());
        assertNotNull(storedBinary.getId());
        assertEquals(9689, storedBinary.getLength());
    }

    @Test
    void shouldVerifyFileParameters() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart("/binary")
                .param("mimeType", "application/pdf"))
                .andExpect(status().is(400));
    }
    @Test
    void shouldVerifyMimeTypeParameters() throws Exception {
        MockMultipartFile file = new MockMultipartFile("data", "Test File.pdf", "application/pdf", getClass().getResourceAsStream("/test_files/A Sample PDF.pdf"));
        mockMvc.perform(MockMvcRequestBuilders.multipart("/binary")
                .file(file))
                .andExpect(status().is(400));
    }
}
package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.mail.JdbcMessageService;
import com.dedicatedcode.paperspace.mail.Message;
import com.dedicatedcode.paperspace.mail.MessageState;
import com.dedicatedcode.paperspace.model.Binary;
import com.dedicatedcode.paperspace.search.SolrService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class DocumentControllerTest {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcMessageService messageService;

    @MockBean
    private SolrService solrService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();
    }

    @Test
    void uploadDocument() throws Exception {
        UUID binaryId = storeBinary().getId();

        DocumentController.DocumentUpload upload = new DocumentController.DocumentUpload();
        upload.setTitle("Test : Document öäü");
        upload.setBinaryId(binaryId);

        mockMvc.perform(post("/document")
                .content(objectMapper.writeValueAsString(upload))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(201))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Test : Document öäü"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.description").value((Object) null))
                .andExpect(jsonPath("$.file.id").value(binaryId.toString()));
    }

    @Test
    void uploadTask() throws Exception {
        UUID binaryId = storeBinary().getId();

        DocumentController.DocumentUpload upload = new DocumentController.DocumentUpload();
        upload.setTitle("Test : Document öäü");
        upload.setBinaryId(binaryId);

        mockMvc.perform(post("/task")
                .content(objectMapper.writeValueAsString(upload))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(201))
                .andDo(print())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Test : Document öäü"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.state").value("OPEN"))
                .andExpect(jsonPath("$.description").value((Object) null))
                .andExpect(jsonPath("$.file.id").value(binaryId.toString()));

    }

    @Test
    void verifyBinary() throws Exception {
        DocumentController.DocumentUpload upload = new DocumentController.DocumentUpload();
        upload.setTitle("Test : Document öäü");
        upload.setBinaryId(UUID.randomUUID());

        mockMvc.perform(post("/task")
                .content(objectMapper.writeValueAsString(upload))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(400));
        mockMvc.perform(post("/document")
                .content(objectMapper.writeValueAsString(upload))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(400));
    }

    @Test
    void verifyTitle() throws Exception {
        UUID binaryId = storeBinary().getId();

        DocumentController.DocumentUpload upload = new DocumentController.DocumentUpload();
        upload.setBinaryId(binaryId);

        mockMvc.perform(post("/task")
                .content(objectMapper.writeValueAsString(upload))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(400));
        mockMvc.perform(post("/document")
                .content(objectMapper.writeValueAsString(upload))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(400));
    }

    @Test
    void shouldSendMessageOnNewDocument() throws Exception {
        UUID binaryId = storeBinary().getId();

        DocumentController.DocumentUpload upload = new DocumentController.DocumentUpload();
        upload.setTitle("Test : Document öäü");
        upload.setBinaryId(binaryId);

        AtomicReference<UUID> uuidAtomicReference = new AtomicReference<>();
        mockMvc.perform(post("/document")
                .content(objectMapper.writeValueAsString(upload))
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is(201))
                .andDo(result -> {
                    JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
                    uuidAtomicReference.set(UUID.fromString(node.get("id").asText()));
                });

        List<Message> messages = this.messageService.getScheduledMessageBy("DOCUMENT_CREATED_" + uuidAtomicReference.get());
        assertEquals(1, messages.size());
        Message message = messages.get(0);
        assertEquals(1, message.getAttachments().size());
        assertEquals("New document uploaded", message.getSubject());
        assertEquals("local-test-recipient@local", message.getRecipient());
        assertEquals(MessageState.SCHEDULED, message.getMessageState());
        assertTrue(message.getBody().contains("https://testing.local/document/" + uuidAtomicReference.get()));
        assertTrue(message.getBody().contains("open document"));
        assertEquals(binaryId, message.getAttachments().get(0).getId());
    }
    @Test
    void shouldSendMessageOnNewTask() throws Exception {
        UUID binaryId = storeBinary().getId();

        DocumentController.DocumentUpload upload = new DocumentController.DocumentUpload();
        upload.setTitle("Test : Document öäü");
        upload.setBinaryId(binaryId);

        AtomicReference<UUID> uuidAtomicReference = new AtomicReference<>();
        mockMvc.perform(post("/task")
                .content(objectMapper.writeValueAsString(upload))
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is(201))
                .andDo(result -> {
                    JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
                    uuidAtomicReference.set(UUID.fromString(node.get("id").asText()));
                });

        List<Message> messages = this.messageService.getScheduledMessageBy("TASK_CREATED_" + uuidAtomicReference.get());
        assertEquals(1, messages.size());
        Message message = messages.get(0);
        assertEquals(1, message.getAttachments().size());
        assertEquals("New task uploaded", message.getSubject());
        assertEquals("local-test-recipient@local", message.getRecipient());
        assertEquals(MessageState.SCHEDULED, message.getMessageState());
        assertTrue(message.getBody().contains("https://testing.local/task/" + uuidAtomicReference.get()));
        assertTrue(message.getBody().contains("open"));
        assertEquals(binaryId, message.getAttachments().get(0).getId());

        List<Message> reminderMessages = this.messageService.getScheduledMessageBy("TASK_DUE_" + uuidAtomicReference.get());
        assertEquals(1, reminderMessages.size());
        Message reminder = reminderMessages.get(0);
        assertEquals(1, reminder.getAttachments().size());
        assertEquals("Task needs attention", reminder.getSubject());
        assertEquals("local-test-recipient@local", reminder.getRecipient());
        assertEquals(MessageState.SCHEDULED, reminder.getMessageState());
        assertTrue(reminder.getBody().contains("https://testing.local/task/" + uuidAtomicReference.get()));
        assertTrue(reminder.getBody().contains("open"));
        assertTrue(reminder.getBody().contains("done"));

        assertTrue(reminder.getBody().contains("https://testing.local/task/" + uuidAtomicReference.get() + "/done"));
        assertTrue(reminder.getBody().contains("done"));

        assertEquals(binaryId, reminder.getAttachments().get(0).getId());
    }

    private Binary storeBinary() throws Exception {
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
        return binary.get();
    }

}
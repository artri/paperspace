package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.model.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.dedicatedcode.paperspace.TestHelper.rand;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(TagController.class)
class TagControllerTest {

    @MockBean
    private TagService tagService;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnAllTags() throws Exception {
        List<Tag> tags = Arrays.asList(
                new Tag(UUID.randomUUID(), rand()),
                new Tag(UUID.randomUUID(), rand()),
                new Tag(UUID.randomUUID(), rand()));
        when(tagService.getAll(null)).thenReturn(
                tags
        );
        mockMvc.perform(get("/api/tags.json"))
                .andDo(print())
                .andExpect(status().is(200))
                .andExpect(jsonPath("[0].id").value(tags.get(0).getId().toString()))
                .andExpect(jsonPath("[0].name").value(tags.get(0).getName()))
                .andExpect(jsonPath("[1].id").value(tags.get(1).getId().toString()))
                .andExpect(jsonPath("[1].name").value(tags.get(1).getName()))
                .andExpect(jsonPath("[2].id").value(tags.get(2).getId().toString()))
                .andExpect(jsonPath("[2].name").value(tags.get(2).getName()));
    }

    @Test
    void shouldReturnAllTagsWIthSearch() throws Exception {
        List<Tag> tags = Arrays.asList(
                new Tag(UUID.randomUUID(), rand()),
                new Tag(UUID.randomUUID(), rand()),
                new Tag(UUID.randomUUID(), rand()));
        when(tagService.getAll("test")).thenReturn(
                tags
        );
        mockMvc.perform(get("/api/tags.json?search=test"))
                .andDo(print())
                .andExpect(status().is(200))
                .andExpect(jsonPath("[0].id").value(tags.get(0).getId().toString()))
                .andExpect(jsonPath("[0].name").value(tags.get(0).getName()))
                .andExpect(jsonPath("[1].id").value(tags.get(1).getId().toString()))
                .andExpect(jsonPath("[1].name").value(tags.get(1).getName()))
                .andExpect(jsonPath("[2].id").value(tags.get(2).getId().toString()))
                .andExpect(jsonPath("[2].name").value(tags.get(2).getName()));
    }
}
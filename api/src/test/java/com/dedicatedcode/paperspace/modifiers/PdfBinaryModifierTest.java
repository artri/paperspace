package com.dedicatedcode.paperspace.modifiers;

import com.dedicatedcode.paperspace.TestHelper;
import com.dedicatedcode.paperspace.model.Binary;
import com.dedicatedcode.paperspace.model.Page;
import com.dedicatedcode.paperspace.web.PageEditModel;
import com.dedicatedcode.paperspace.web.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIf(
        expression = "#{systemProperties['os.name'].toLowerCase().contains('linux')}",
        reason = "Enabled on Linux"
)
class PdfBinaryModifierTest {

    @Test
    void shouldNotModifyIfNoChanges() {
        PdfBinaryModifier modifier = new PdfBinaryModifier("stapler");
        assertTrue(modifier.isEnabled());

        Binary binary = TestHelper.randBinary(10);
        List<Page> pages = IntStream.of(10).mapToObj(i -> new Page(UUID.randomUUID(), i, "TEST CONTENT", null)).collect(Collectors.toList());
        List<PageEditModel> originalModel = pages.stream().map(page -> new PageEditModel(new PageResponse(page), Collections.emptyList())).collect(Collectors.toList());
        assertNull(modifier.modify(binary, originalModel, originalModel));
    }

    @Test
    void shouldResortPdf() {
        PdfBinaryModifier modifier = new PdfBinaryModifier("stapler");
        assertTrue(modifier.isEnabled());

        Binary binary = TestHelper.randBinary(10);
        List<Page> pages = IntStream.range(1, 11).mapToObj(i -> new Page(UUID.randomUUID(), i, "TEST CONTENT", null)).collect(Collectors.toList());
        List<PageEditModel> originalModel = pages.stream().map(page -> new PageEditModel(new PageResponse(page), Collections.emptyList())).collect(Collectors.toList());
        List<PageEditModel> changedModel = originalModel.stream().sorted(Comparator.comparing(pageEditModel -> pageEditModel.getPage().getNumber())).collect(Collectors.toList());
        Collections.reverse(changedModel);
        assertNull(modifier.modify(binary, originalModel, changedModel));
    }

}
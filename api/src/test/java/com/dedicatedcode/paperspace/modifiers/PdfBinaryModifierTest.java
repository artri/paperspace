package com.dedicatedcode.paperspace.modifiers;

import com.dedicatedcode.paperspace.TestHelper;
import com.dedicatedcode.paperspace.model.Binary;
import com.dedicatedcode.paperspace.model.Page;
import com.dedicatedcode.paperspace.web.PageEditModel;
import com.dedicatedcode.paperspace.web.PageEditTransformation;
import com.dedicatedcode.paperspace.web.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.util.DigestUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class PdfBinaryModifierTest {

    @Test
    void shouldNotModifyIfNoChanges() throws IOException {
        PdfBinaryModifier modifier = new PdfBinaryModifier("stapler");
        assertTrue(modifier.isEnabled());

        Binary binary = TestHelper.randBinary(10);
        String oldHash = binary.getHash();

        List<Page> pages = IntStream.range(1, 11).mapToObj(i -> new Page(UUID.randomUUID(), i, "TEST CONTENT", null)).collect(Collectors.toList());
        List<PageEditModel> originalModel = pages.stream().map(page -> new PageEditModel(new PageResponse(page), Collections.emptyList())).collect(Collectors.toList());
        assertFalse(modifier.modify(binary, originalModel, originalModel));

        InputStream is = new FileInputStream(binary.getStorageLocation());
        String newHash = DigestUtils.md5DigestAsHex(is);
        assertEquals(oldHash, newHash);
    }

    @Test
    void shouldResortPdf() throws IOException {
        PdfBinaryModifier modifier = new PdfBinaryModifier("stapler");
        assertTrue(modifier.isEnabled());

        Binary binary = TestHelper.randBinary(10);

        String oldHash = binary.getHash();
        List<Page> pages = IntStream.range(1, 11).mapToObj(i -> new Page(UUID.randomUUID(), i, "TEST CONTENT", null)).collect(Collectors.toList());
        List<PageEditModel> originalModel = pages.stream().map(page -> new PageEditModel(new PageResponse(page), Collections.emptyList())).collect(Collectors.toList());
        List<PageEditModel> changedModel = originalModel.stream().sorted(Comparator.comparing(pageEditModel -> pageEditModel.getPage().getNumber())).collect(Collectors.toList());
        Collections.reverse(changedModel);
        assertTrue(modifier.modify(binary, originalModel, changedModel));

        InputStream is = new FileInputStream(binary.getStorageLocation());
        String newHash = DigestUtils.md5DigestAsHex(is);
        assertNotEquals(oldHash, newHash);
    }

    @Test
    void shouldChangeOnRotationResortPdf() throws IOException {
        PdfBinaryModifier modifier = new PdfBinaryModifier("stapler");
        assertTrue(modifier.isEnabled());

        Binary binary = TestHelper.randBinary(10);

        String oldHash = binary.getHash();
        List<Page> pages = IntStream.range(1, 11).mapToObj(i -> new Page(UUID.randomUUID(), i, "TEST CONTENT", null)).collect(Collectors.toList());
        List<PageEditModel> originalModel = pages.stream().map(page -> new PageEditModel(new PageResponse(page), new ArrayList<>())).collect(Collectors.toList());
        List<PageEditModel> changedModel = new ArrayList<>(originalModel);
        changedModel.get(2).getTransformations().add(PageEditTransformation.ROTATE_CLOCKWISE);
        changedModel.get(2).getTransformations().add(PageEditTransformation.ROTATE_CLOCKWISE);
        changedModel.get(2).getTransformations().add(PageEditTransformation.ROTATE_COUNTER_CLOCKWISE);

        assertTrue(modifier.modify(binary, originalModel, changedModel));

        InputStream is = new FileInputStream(binary.getStorageLocation());
        String newHash = DigestUtils.md5DigestAsHex(is);
        assertNotEquals(oldHash, newHash);
    }

}
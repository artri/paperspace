package com.dedicatedcode.paperspace.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SolrQueryBuilderTest {

    private final SolrQueryBuilder builder = new SolrQueryBuilder();

    @Test
    void shouldBuildEmptyQuery() {
        assertEquals("*:*", builder.build(null));
        assertEquals("*:*", builder.build(""));
    }

    @Test
    void shouldBuildSimpleQuery() {
        assertEquals("+(title:\"2020\"^10 OR description:\"2020\"^5 OR content:\"2020\"^2)", builder.build("2020"));
    }

    @Test
    void shouldBuildAndQuery() {
        assertEquals("+(title:\"2020\"^10 OR description:\"2020\"^5 OR content:\"2020\"^2)\n" +
                "+(title:\"test\"^10 OR description:\"test\"^5 OR content:\"test\"^2)", builder.build("2020 +Test"));
    }

    @Test
    void shouldBuildNotQuery() {
        assertEquals("+(title:\"2020\"^10 OR description:\"2020\"^5 OR content:\"2020\"^2)\n" +
                "-(title:\"test\"^10 OR description:\"test\"^5 OR content:\"test\"^2)", builder.build("2020 -Test"));
    }

    @Test
    void shouldHandleDashInQueryString() {
        assertEquals("+(title:\"sachversicherungs-ag\"^10 OR description:\"sachversicherungs-ag\"^5 OR content:\"sachversicherungs-ag\"^2)",
                builder.build("Sachversicherungs-AG"));
    }

    @Test
    void shouldHandleDashInQueryString() {
        assertEquals("+(title:sachversicherungs-ag^10 OR description:sachversicherungs-ag^5 OR content:sachversicherungs-ag^2)",
                builder.build("Sachversicherungs-AG"));
    }

    @Test
    void shouldEscapeSpecialCharactersBuildNotQuery() {
        assertEquals("+(title:\"+\\(2020\\) -test\"^10 OR description:\"+\\(2020\\) -test\"^5 OR content:\"+\\(2020\\) -test\"^2)", builder.build("\\+(2020) \\-Test"));
    }
}
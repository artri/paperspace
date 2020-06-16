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
        assertEquals("+(title:*2020*^10 OR description:*2020*^5 OR content:*2020*^2)", builder.build("2020"));
    }

    @Test
    void shouldBuildAndQuery() {
        assertEquals("+(title:*2020*^10 OR description:*2020*^5 OR content:*2020*^2)\n" +
                "+(title:*Test*^10 OR description:*Test*^5 OR content:*Test*^2)", builder.build("2020 +Test"));
    }

    @Test
    void shouldBuildNotQuery() {
        assertEquals("+(title:*2020*^10 OR description:*2020*^5 OR content:*2020*^2)\n" +
                "-(title:*Test*^10 OR description:*Test*^5 OR content:*Test*^2)", builder.build("2020 -Test"));
    }

    @Test
    void shouldEscapeSpecialCharactersBuildNotQuery() {
        assertEquals("+(title:*+\\(2020\\) -Test*^10 OR description:*+\\(2020\\) -Test*^5 OR content:*+\\(2020\\) -Test*^2)", builder.build("\\+(2020) \\-Test"));
    }
}
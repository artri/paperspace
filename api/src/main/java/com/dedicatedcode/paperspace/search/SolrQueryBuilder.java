package com.dedicatedcode.paperspace.search;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

@Component
public class SolrQueryBuilder {
    public String build(String queryString) {
        if (queryString == null || queryString.trim().equals("")) {
            return "*:*";
        } else {
            queryString = normalizeQueryString(queryString);
            char[] chars = escapeSpecialCharacters(queryString).toCharArray();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(chars[0]);

            for (int i = 1; i < chars.length - 1; i++) {
                char before = chars[i - 1];
                char current = chars[i];

                CharType typeBefore = CharType.type(before);
                CharType typeCurrent = CharType.type(current);

                switch (typeCurrent) {
                    case ESCAPE:
                        stringBuilder.append("\\");
                    case SPACE:
                    case CHAR:
                        if (current == '(' || current == ')') {
                            stringBuilder.append("\\");
                        }
                        stringBuilder.append(current);
                        break;
                    case KEYWORD:
                        if (typeBefore == CharType.ESCAPE || typeBefore == CharType.CHAR)
                            stringBuilder.append(current == '+' ? "__plus__" : "__minus__");
                        else
                            stringBuilder.append(current);
                        break;
                }
            }
            stringBuilder.append(chars[chars.length - 1]);

            queryString = stringBuilder.toString();
            StringTokenizer tokenizer = new StringTokenizer(queryString, "+-", true);
            List<String> tokens = new ArrayList<>();
            while (tokenizer.hasMoreElements()) {
                tokens.add(tokenizer.nextToken().trim());
            }
            List<QueryPart> parts = createParts(tokens);
            return parts.stream().map(QueryPart::createPart).map(this::unescapeSpecialCharacters).collect(Collectors.joining("\n"));
        }
    }

    private List<QueryPart> createParts(List<String> tokens) {
        List<QueryPart> parts = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i = i + 2) {
            if (i + 1 < tokens.size()) {
                parts.add(new QueryPart(Delimiter.fromToken(tokens.get(i)), tokens.get(i + 1)));
            }
        }
        return parts;
    }

    private String normalizeQueryString(String queryString) {
        String result;
        if (queryString.startsWith("+")) {
            result = queryString;
        } else {
            result = "+" + queryString;
        }
        return result;
    }

    private String escapeSpecialCharacters(String query) {
        return query
                .replaceAll("\\\\[+]", "__plus__")
                .replaceAll("\\\\[-]", "__minus__")
                .replaceAll("\\(", "__par_start__")
                .replaceAll("\\)", "__par_end__")
                .replaceAll("\\*", "__star__");
    }

    private String unescapeSpecialCharacters(String query) {
        return query
                .replaceAll("__plus__", "+")
                .replaceAll("__minus__", "-")
                .replaceAll("__par_start__", "\\\\(")
                .replaceAll("__par_end__", "\\\\)")
                .replaceAll("__star__", "\\\\*");
    }

    private enum Delimiter {
        AND("+"),
        AND_NOT("-");

        private final String delimiter;

        Delimiter(String delimiter) {
            this.delimiter = delimiter;
        }

        static Delimiter fromToken(String token) {
            for (Delimiter value : values()) {
                if (value.delimiter.equals(token)) {
                    return value;
                }
            }
            throw new RuntimeException("Unable to handle token [" + token + "]");
        }
    }

    private static class QueryPart {

        private final Delimiter delimiter;
        private final String searchKey;

        public QueryPart(Delimiter delimiter, String searchKey) {
            this.delimiter = delimiter;
            this.searchKey = searchKey.toLowerCase();
        }

        public String createPart() {
            return delimiter.delimiter + "(title:\"" + searchKey + "\"^10 OR description:\"" + searchKey + "\"^5 OR content:\"" + searchKey + "\"^2)";

        }
    }

    private enum CharType {
        ESCAPE,
        SPACE,
        CHAR,
        KEYWORD;

        public static CharType type(char character) {
            switch (character) {
                case ' ':
                    return SPACE;
                case '-':
                case '+':
                    return KEYWORD;
                case '\\':
                    return ESCAPE;
                default:
                    return CHAR;
            }
        }
    }
}

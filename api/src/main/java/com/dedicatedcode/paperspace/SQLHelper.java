package com.dedicatedcode.paperspace;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public final class SQLHelper {
    private SQLHelper() {
    }

    public static LocalDateTime nullableDateTime(ResultSet rs, String columnName) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnName);
        if (timestamp == null) {
            return null;
        } else {
            return timestamp.toLocalDateTime();
        }
    }
    public static Timestamp nullableTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        } else {
            return Timestamp.valueOf(dateTime);
        }
    }

    public static String nullableString(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        if (value == null || value.isEmpty()) {
            return null;
        } else {
            return value;
        }
    }

}

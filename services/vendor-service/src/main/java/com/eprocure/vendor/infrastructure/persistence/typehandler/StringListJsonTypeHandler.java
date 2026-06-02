package com.eprocure.vendor.infrastructure.persistence.typehandler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(List.class)
@MappedJdbcTypes(value = JdbcType.VARCHAR, includeNullJdbcType = true)
public class StringListJsonTypeHandler extends BaseTypeHandler<List<String>> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    @Override
    public void setNonNullParameter(PreparedStatement ps, int index, List<String> parameter, JdbcType jdbcType)
            throws SQLException {
        try {
            ps.setString(index, OBJECT_MAPPER.writeValueAsString(parameter));
        } catch (Exception exception) {
            throw new SQLException("Unable to serialize JSON string list", exception);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private List<String> parse(String value) throws SQLException {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return List.copyOf(OBJECT_MAPPER.readValue(value, STRING_LIST));
        } catch (Exception exception) {
            throw new SQLException("Unable to parse JSON string list", exception);
        }
    }
}

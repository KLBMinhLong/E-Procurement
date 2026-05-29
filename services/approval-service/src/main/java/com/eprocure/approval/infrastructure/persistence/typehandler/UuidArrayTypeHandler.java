package com.eprocure.approval.infrastructure.persistence.typehandler;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(UUID[].class)
@MappedJdbcTypes(value = JdbcType.ARRAY, includeNullJdbcType = true)
public class UuidArrayTypeHandler extends BaseTypeHandler<UUID[]> {
    private static final String POSTGRES_UUID_ARRAY_TYPE = "uuid";

    @Override
    public void setNonNullParameter(PreparedStatement ps, int index, UUID[] parameter, JdbcType jdbcType)
            throws SQLException {
        Array array = ps.getConnection().createArrayOf(POSTGRES_UUID_ARRAY_TYPE, parameter);
        ps.setArray(index, array);
    }

    @Override
    public UUID[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseArray(rs.getArray(columnName));
    }

    @Override
    public UUID[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseArray(rs.getArray(columnIndex));
    }

    @Override
    public UUID[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseArray(cs.getArray(columnIndex));
    }

    private UUID[] parseArray(Array array) throws SQLException {
        if (array == null) {
            return null;
        }
        Object rawArray = array.getArray();
        if (rawArray instanceof UUID[] values) {
            return values;
        }
        if (rawArray instanceof Object[] values) {
            UUID[] result = new UUID[values.length];
            for (int index = 0; index < values.length; index++) {
                result[index] = parseUuid(values[index]);
            }
            return result;
        }
        return new UUID[] { parseUuid(rawArray) };
    }

    private UUID parseUuid(Object value) throws SQLException {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException ex) {
            throw new SQLException("Invalid UUID value from PostgreSQL array", ex);
        }
    }
}

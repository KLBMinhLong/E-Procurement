package com.eprocure.approval.infrastructure.persistence.typehandler;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(String[].class)
@MappedJdbcTypes(value = JdbcType.ARRAY, includeNullJdbcType = true)
public class StringArrayTypeHandler extends BaseTypeHandler<String[]> {
    private static final String POSTGRES_VARCHAR_ARRAY_TYPE = "varchar";

    @Override
    public void setNonNullParameter(PreparedStatement ps, int index, String[] parameter, JdbcType jdbcType)
            throws SQLException {
        Array array = ps.getConnection().createArrayOf(POSTGRES_VARCHAR_ARRAY_TYPE, parameter);
        ps.setArray(index, array);
    }

    @Override
    public String[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseArray(rs.getArray(columnName));
    }

    @Override
    public String[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseArray(rs.getArray(columnIndex));
    }

    @Override
    public String[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseArray(cs.getArray(columnIndex));
    }

    private String[] parseArray(Array array) throws SQLException {
        if (array == null) {
            return null;
        }
        Object rawArray = array.getArray();
        if (rawArray instanceof String[] values) {
            return values;
        }
        if (rawArray instanceof Object[] values) {
            String[] result = new String[values.length];
            for (int index = 0; index < values.length; index++) {
                result[index] = values[index] == null ? null : values[index].toString();
            }
            return result;
        }
        return new String[] { rawArray.toString() };
    }
}

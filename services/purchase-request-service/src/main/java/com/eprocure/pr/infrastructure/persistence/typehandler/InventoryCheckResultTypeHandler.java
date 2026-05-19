package com.eprocure.pr.infrastructure.persistence.typehandler;

import com.eprocure.pr.domain.model.vo.InventoryCheckResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(InventoryCheckResult.class)
@MappedJdbcTypes(value = JdbcType.OTHER, includeNullJdbcType = true)
public class InventoryCheckResultTypeHandler extends BaseTypeHandler<InventoryCheckResult> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public void setNonNullParameter(PreparedStatement ps, int index, InventoryCheckResult parameter, JdbcType jdbcType)
            throws SQLException {
        try {
            ps.setObject(index, OBJECT_MAPPER.writeValueAsString(parameter), Types.OTHER);
        } catch (Exception exception) {
            throw new SQLException("Unable to serialize inventory check result", exception);
        }
    }

    @Override
    public InventoryCheckResult getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public InventoryCheckResult getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public InventoryCheckResult getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private InventoryCheckResult parse(String value) throws SQLException {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(value, InventoryCheckResult.class);
        } catch (Exception exception) {
            throw new SQLException("Unable to parse inventory check result", exception);
        }
    }
}

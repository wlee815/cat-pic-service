package com.william.core;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CatPicMapper implements ResultSetMapper<CatPic> {
    public CatPic map(int index, ResultSet resultSet, StatementContext statementContext) throws SQLException {
        return new CatPic(resultSet.getInt("id"), resultSet.getString("file_path"));
    }
}


package com.william.db;

import com.william.core.CatPic;
import com.william.core.CatPicMapper;
import org.skife.jdbi.v2.sqlobject.*;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;

public interface CatPicDao {
    @SqlQuery("select id from cat_pics")
    List<Integer> getIds();

    @SqlQuery("select * from cat_pics where id = :id")
    @RegisterMapper(CatPicMapper.class)
    CatPic findByid(@Bind("id") int id);

    @SqlUpdate("delete from cat_pics where id = :id")
    int deleteByid(@Bind("id") int id);

    @SqlUpdate("update cat_pics set file_path = :file_path where id = :id")
    int update(@Bind("id") int id, @Bind("file_path") String filePath);

    @SqlUpdate("insert into cat_pics (file_path) values (:file_path)")
    @GetGeneratedKeys
    int insert(@Bind("file_path") String filePath);
}

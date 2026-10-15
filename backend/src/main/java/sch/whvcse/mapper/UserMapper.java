package sch.whvcse.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import sch.whvcse.entity.User;

/**
 * 用户持久层。用注解写 SQL，也可改成 XML（见 application.properties 的 mapper-locations）。
 */
@Mapper
public interface UserMapper {

    @Select("SELECT id, username, password, role, name FROM user WHERE username = #{username}")
    User findByUsername(String username);

    @Insert("INSERT INTO user(username, password, role, name) VALUES(#{username}, #{password}, #{role}, #{name})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Select("SELECT COUNT(*) FROM user")
    long count();
}

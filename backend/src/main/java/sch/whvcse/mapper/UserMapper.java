package sch.whvcse.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import sch.whvcse.entity.User;

/** sys_user 登录用户 */
@Mapper
public interface UserMapper {

    String COLS = "id, username, password, name, role, status, create_time, del_flag";

    @Select("SELECT " + COLS + " FROM sys_user WHERE username = #{username} AND del_flag = 0")
    User findByUsername(String username);

    @Select("SELECT " + COLS + " FROM sys_user WHERE id = #{id} AND del_flag = 0")
    User findById(Long id);

    @Select("SELECT " + COLS + " FROM sys_user WHERE del_flag = 0 ORDER BY id")
    List<User> findAll();

    /** 用户管理页的多条件查询 */
    @Select("""
            <script>
            SELECT id, username, password, name, role, status, create_time, del_flag
            FROM sys_user
            WHERE del_flag = 0
            <if test="keyword != null and keyword != ''">
              AND (username LIKE CONCAT('%', #{keyword}, '%') OR name LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="role != null and role != ''"> AND role = #{role} </if>
            ORDER BY id
            </script>
            """)
    List<User> search(@Param("keyword") String keyword, @Param("role") String role);

    @Insert("""
            INSERT INTO sys_user(username, password, name, role, status)
            VALUES(#{username}, #{password}, #{name}, #{role}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE sys_user SET name = #{name}, role = #{role} WHERE id = #{id}")
    int update(User user);

    @Update("UPDATE sys_user SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE sys_user SET password = #{password} WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    /** 逻辑删除 */
    @Update("UPDATE sys_user SET del_flag = 1 WHERE id = #{id}")
    int softDelete(Long id);

    /** 彻底删除（无外键引用时使用） */
    @Delete("DELETE FROM sys_user WHERE id = #{id}")
    int hardDelete(Long id);

    @Select("SELECT COUNT(*) FROM sys_user")
    long count();
}

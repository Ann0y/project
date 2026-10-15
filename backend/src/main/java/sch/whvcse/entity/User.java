package sch.whvcse.entity;

import lombok.Data;

/**
 * 用户表实体，对应数据库 user 表。
 */
@Data
public class User {

    private Long id;
    private String username;
    /** 加密后的密码（BCrypt） */
    private String password;
    /** 角色：admin / student */
    private String role;
    /** 显示名 */
    private String name;
}

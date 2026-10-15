package sch.whvcse;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 后端启动类。
 * 运行本类的 main 方法，或用 mvn spring-boot:run 即可启动内嵌 Tomcat。
 */
@SpringBootApplication
@MapperScan("sch.whvcse.mapper")
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}

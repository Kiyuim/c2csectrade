package lut.cn.c2cplatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("lut.cn.c2cplatform.mapper") // 扫描 Mapper 接口
public class C2cPlatformApplication {
    public static void main(String[] args) {  // ✅ 正确写法
        SpringApplication.run(C2cPlatformApplication.class, args);
    }
}
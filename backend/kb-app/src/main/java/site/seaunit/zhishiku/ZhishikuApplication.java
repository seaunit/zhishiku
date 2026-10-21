package site.seaunit.zhishiku;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ZhishikuApplication {
  public static void main(String[] args) {
    SpringApplication.run(ZhishikuApplication.class, args);
  }
}

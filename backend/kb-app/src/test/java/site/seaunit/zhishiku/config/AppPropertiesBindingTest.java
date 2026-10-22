package site.seaunit.zhishiku.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class AppPropertiesBindingTest {
  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withInitializer(new ConfigDataApplicationContextInitializer())
          .withUserConfiguration(TestConfiguration.class);

  @Test
  void bindsRecordWhenConvenienceConstructorsArePresent() {
    contextRunner.run(
        context -> {
          assertThat(context).hasNotFailed();

          AppProperties properties = context.getBean(AppProperties.class);
          assertThat(properties.ai().model()).isEqualTo("deepseek-chat");
          assertThat(properties.registration().resendCooldownSeconds()).isEqualTo(60);
          assertThat(properties.billing().enabled()).isFalse();
        });
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(AppProperties.class)
  static class TestConfiguration {}
}

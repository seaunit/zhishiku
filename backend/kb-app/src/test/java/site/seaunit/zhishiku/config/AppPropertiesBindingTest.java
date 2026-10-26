package site.seaunit.zhishiku.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import site.seaunit.zhishiku.ai.orchestration.AiOrchestrationProperties;

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
          assertThat(properties.webImport()).isNotNull();
          assertThat(properties.webImport().maxItems()).isEqualTo(20);
          assertThat(properties.registration().resendCooldownSeconds()).isEqualTo(60);
          assertThat(properties.billing().enabled()).isFalse();
          AiOrchestrationProperties orchestration =
              context.getBean(AiOrchestrationProperties.class);
          assertThat(orchestration.provider()).isEqualTo("deepseek");
          assertThat(orchestration.timeoutSeconds()).isEqualTo(60);
        });
  }

  @Test
  void prefersShihaiPrefixedEnvironmentNames() {
    contextRunner
        .withPropertyValues(
            "SHIHAI_BILLING_ENABLED=true",
            "SHIHAI_BILLING_TRIAL_POINTS=321",
            "SHIHAI_WEB_IMPORT_ALLOW_UNBILLED_AI=true")
        .run(
            context -> {
              assertThat(context).hasNotFailed();

              AppProperties properties = context.getBean(AppProperties.class);
              assertThat(properties.billing().enabled()).isTrue();
              assertThat(properties.billing().trialPoints()).isEqualTo(321);
              assertThat(properties.webImport().allowUnbilledAi()).isTrue();
            });
  }

  @Test
  void keepsLegacyEnvironmentNamesWorkingDuringMigration() {
    contextRunner
        .withPropertyValues(
            "BILLING_ENABLED=true",
            "BILLING_TRIAL_POINTS=222",
            "KB_WEB_IMPORT_ALLOW_UNBILLED_AI=true")
        .run(
            context -> {
              assertThat(context).hasNotFailed();

              AppProperties properties = context.getBean(AppProperties.class);
              assertThat(properties.billing().enabled()).isTrue();
              assertThat(properties.billing().trialPoints()).isEqualTo(222);
              assertThat(properties.webImport().allowUnbilledAi()).isTrue();
            });
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties({AppProperties.class, AiOrchestrationProperties.class})
  static class TestConfiguration {}
}

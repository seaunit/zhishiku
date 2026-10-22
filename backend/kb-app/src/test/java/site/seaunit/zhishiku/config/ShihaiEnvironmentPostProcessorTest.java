package site.seaunit.zhishiku.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.SpringFactoriesLoader;

class ShihaiEnvironmentPostProcessorTest {
  private final ShihaiEnvironmentPostProcessor processor = new ShihaiEnvironmentPostProcessor();

  @Test
  void isRegisteredForSpringApplicationStartup() {
    assertThat(
            SpringFactoriesLoader.loadFactoryNames(
                EnvironmentPostProcessor.class, getClass().getClassLoader()))
        .contains(ShihaiEnvironmentPostProcessor.class.getName());
  }

  @Test
  void acceptsShihaiPrefixedNamesWithoutLegacyAliases() {
    StandardEnvironment environment = environment(Map.of("SHIHAI_APP_ENV", "production"));

    assertThatCode(() -> processor.postProcessEnvironment(environment, new SpringApplication()))
        .doesNotThrowAnyException();
  }

  @Test
  void acceptsLegacyNamesDuringMigration() {
    StandardEnvironment environment = environment(Map.of("APP_ENV", "production"));

    assertThatCode(() -> processor.postProcessEnvironment(environment, new SpringApplication()))
        .doesNotThrowAnyException();
  }

  @Test
  void acceptsMatchingCanonicalAndLegacyValues() {
    StandardEnvironment environment =
        environment(
            Map.of(
                "SHIHAI_BILLING_ENABLED", "false",
                "BILLING_ENABLED", "false"));

    assertThatCode(() -> processor.postProcessEnvironment(environment, new SpringApplication()))
        .doesNotThrowAnyException();
  }

  @Test
  void fallsBackToLegacyValueWhenCanonicalValueIsBlank() {
    StandardEnvironment environment =
        environment(
            Map.of(
                "SHIHAI_BILLING_ENABLED", "",
                "BILLING_ENABLED", "true"));

    processor.postProcessEnvironment(environment, new SpringApplication());

    assertThat(environment.getProperty("SHIHAI_BILLING_ENABLED")).isEqualTo("true");
    assertThat(environment.getProperty("BILLING_ENABLED")).isEqualTo("true");
  }

  @Test
  void rejectsConflictingCanonicalAndLegacyValuesWithoutExposingSecrets() {
    StandardEnvironment environment =
        environment(
            Map.of(
                "SHIHAI_DEEPSEEK_API_KEY", "new-secret",
                "DEEPSEEK_API_KEY", "legacy-secret"));

    assertThatThrownBy(() -> processor.postProcessEnvironment(environment, new SpringApplication()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("SHIHAI_DEEPSEEK_API_KEY")
        .hasMessageContaining("DEEPSEEK_API_KEY")
        .hasMessageNotContaining("new-secret")
        .hasMessageNotContaining("legacy-secret");
  }

  private static StandardEnvironment environment(Map<String, Object> values) {
    StandardEnvironment environment = new StandardEnvironment();
    environment.getPropertySources().addFirst(new MapPropertySource("migration-test", values));
    return environment;
  }
}

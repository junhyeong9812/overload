package io.github.junhyeong9812.overload.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Overload configuration properties.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "overload")
public class OverloadProperties {

  private boolean enabled = true;
  private Dashboard dashboard = new Dashboard();
  private Defaults defaults = new Defaults();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Dashboard getDashboard() {
    return dashboard;
  }

  public void setDashboard(Dashboard dashboard) {
    this.dashboard = dashboard;
  }

  public Defaults getDefaults() {
    return defaults;
  }

  public void setDefaults(Defaults defaults) {
    this.defaults = defaults;
  }

  public static class Dashboard {
    private String path = "/overload";
    private String title = "Overload - Load Test Dashboard";

    public String getPath() {
      return path;
    }

    public void setPath(String path) {
      this.path = path;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }
  }

  public static class Defaults {
    private int concurrency = 10;
    private int requests = 100;
    private Duration timeout = Duration.ofSeconds(30);

    public int getConcurrency() {
      return concurrency;
    }

    public void setConcurrency(int concurrency) {
      this.concurrency = concurrency;
    }

    public int getRequests() {
      return requests;
    }

    public void setRequests(int requests) {
      this.requests = requests;
    }

    public Duration getTimeout() {
      return timeout;
    }

    public void setTimeout(Duration timeout) {
      this.timeout = timeout;
    }
  }
}
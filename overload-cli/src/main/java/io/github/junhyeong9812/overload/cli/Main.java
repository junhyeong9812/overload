package io.github.junhyeong9812.overload.cli;

import io.github.junhyeong9812.overload.cli.command.OverloadCommand;
import picocli.CommandLine;

/**
 * Overload CLI 진입점.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public class Main {

  public static void main(String[] args) {
    int exitCode = new CommandLine(new OverloadCommand())
        .setCaseInsensitiveEnumValuesAllowed(true)
        .execute(args);
    System.exit(exitCode);
  }
}
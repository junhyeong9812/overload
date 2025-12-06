package io.github.junhyeong9812.overload.cli.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Overload CLI 루트 커맨드.
 *
 * <p>버전 정보와 서브커맨드를 관리한다.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
@Command(
    name = "overload",
    description = "Virtual Thread based HTTP Load Testing Tool",
    version = "Overload v1.0.0",
    mixinStandardHelpOptions = true,
    subcommands = {
        RunCommand.class,
        CommandLine.HelpCommand.class
    }
)
public class OverloadCommand implements Runnable {

  @Override
  public void run() {
    // 서브커맨드 없이 실행 시 도움말 출력
    CommandLine.usage(this, System.out);
  }
}
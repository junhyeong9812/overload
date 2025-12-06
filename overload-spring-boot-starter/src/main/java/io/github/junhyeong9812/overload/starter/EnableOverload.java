package io.github.junhyeong9812.overload.starter;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Overload Load Test Dashboard activation annotation.
 *
 * <pre>{@code
 * @SpringBootApplication
 * @EnableOverload
 * public class Application {
 *     public static void main(String[] args) {
 *         SpringApplication.run(Application.class, args);
 *     }
 * }
 * }</pre>
 *
 * Access dashboard at {@code /overload} after activation.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(OverloadAutoConfiguration.class)
public @interface EnableOverload {
}
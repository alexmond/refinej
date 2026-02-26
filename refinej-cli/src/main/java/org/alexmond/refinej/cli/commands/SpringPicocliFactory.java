package org.alexmond.refinej.cli.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

/**
 * Picocli {@link CommandLine.IFactory} that resolves command instances from the Spring
 * application context, enabling {@code @Autowired} injection into Picocli commands.
 *
 * <p>Falls back to Picocli's default factory for any class not registered as a Spring bean.
 */
@Component
@RequiredArgsConstructor
public class SpringPicocliFactory implements CommandLine.IFactory {

    private final ApplicationContext ctx;

    @Override
    public <K> K create(Class<K> cls) throws Exception {
        try {
            return ctx.getBean(cls);
        } catch (Exception e) {
            return CommandLine.defaultFactory().create(cls);
        }
    }
}

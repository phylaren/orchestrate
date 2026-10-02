package genius.project.orchestrate.common.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.List;
import java.util.regex.Pattern;

public class SensitiveDataMaskingConverter extends ClassicConverter {

    private static final List<MaskingRule> RULES = List.of(
            new MaskingRule(
                    Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b"),
                    "***EMAIL***"),
            new MaskingRule(
                    Pattern.compile("\\beyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"),
                    "***JWT***")
    );

    @Override
    public String convert(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        if (message == null) {
            return "";
        }
        for (MaskingRule rule : RULES) {
            message = rule.apply(message);
        }
        return message;
    }

    private record MaskingRule(Pattern pattern, String replacement) {
        String apply(String input) {
            return pattern.matcher(input).replaceAll(replacement);
        }
    }
}
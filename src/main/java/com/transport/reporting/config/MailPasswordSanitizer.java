package com.transport.reporting.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

/**
 * Nettoie le mot de passe SMTP (guillemets issus de application.properties)
 * avant utilisation par {@link JavaMailSenderImpl}.
 */
@Component
public class MailPasswordSanitizer implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(MailPasswordSanitizer.class);

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof JavaMailSenderImpl sender) {
            String password = sender.getPassword();
            String cleaned = stripOptionalQuotes(password);
            if (password != null && !password.equals(cleaned)) {
                sender.setPassword(cleaned);
                log.info("SMTP password sanitized (quotes removed), length={}",
                        cleaned != null ? cleaned.length() : 0);
            } else {
                log.info("SMTP JavaMailSender ready: host={}, port={}, username={}, passwordLength={}",
                        sender.getHost(), sender.getPort(), sender.getUsername(),
                        password != null ? password.length() : 0);
            }
        }
        return bean;
    }

    private static String stripOptionalQuotes(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 2
                && ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'")))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}

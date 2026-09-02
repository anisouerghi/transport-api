package com.transport.reporting.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * Active Google OAuth uniquement lorsque toutes les propriétés requises sont renseignées.
 */
public class GoogleOAuthConfiguredCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        var env = context.getEnvironment();
        return StringUtils.hasText(env.getProperty("app.google.client-id"))
                && StringUtils.hasText(env.getProperty("app.google.client-secret"))
                && StringUtils.hasText(env.getProperty("app.google.redirect-uri"))
                && StringUtils.hasText(env.getProperty("app.google.frontend-callback-url"));
    }
}

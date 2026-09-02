package com.transport.reporting.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Applique les patches SQL avant Hibernate ({@code ddl-auto=validate} en prod).
 */
@Configuration
@ConditionalOnClass({DataSource.class, JdbcTemplate.class})
@AutoConfigureBefore(HibernateJpaAutoConfiguration.class)
public class EarlyDatabaseSchemaPatchConfiguration {

    @Bean(name = "databaseSchemaPatched")
    public Object databaseSchemaPatched(DataSource dataSource) {
        DatabaseSchemaPatcher.apply(new JdbcTemplate(dataSource));
        return new Object();
    }

    @Bean
    static BeanFactoryPostProcessor entityManagerFactoryDependsOnSchemaPatch() {
        return new BeanFactoryPostProcessor() {
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                if (!beanFactory.containsBeanDefinition("entityManagerFactory")) {
                    return;
                }
                BeanDefinition definition = beanFactory.getBeanDefinition("entityManagerFactory");
                definition.setDependsOn(mergeDependsOn(definition.getDependsOn(), "databaseSchemaPatched"));
            }
        };
    }

    private static String[] mergeDependsOn(String[] existing, String additional) {
        if (existing == null || existing.length == 0) {
            return new String[] {additional};
        }
        for (String name : existing) {
            if (additional.equals(name)) {
                return existing;
            }
        }
        String[] merged = new String[existing.length + 1];
        System.arraycopy(existing, 0, merged, 0, existing.length);
        merged[existing.length] = additional;
        return merged;
    }
}

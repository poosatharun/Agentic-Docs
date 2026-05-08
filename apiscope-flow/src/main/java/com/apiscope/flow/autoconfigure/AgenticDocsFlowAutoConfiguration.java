package com.apiscope.flow.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.apiscope.flow.sql.FlowStatementInspector;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.client.RestClient;

/**
 * Spring Boot AutoConfiguration for the Agentic Docs Flow Tracer.
 *
 * <p>Activated only when:
 * <ul>
 *   <li>The application is a Servlet web application.</li>
 *   <li>{@code apiscope.flow.enabled=true} is set in {@code application.properties}.</li>
 * </ul>
 *
 * <p>{@code matchIfMissing=false} means Flow Tracer is <strong>OFF by default</strong>
 * — teams must explicitly opt in.
 *
 * <p>Registers shared infrastructure beans ({@code ObjectMapper}, {@code RestClient})
 * only when the host application has not already provided them, satisfying
 * {@code @ConditionalOnMissingBean}.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(
        prefix      = "apiscope.flow",
        name        = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
@EnableAspectJAutoProxy
@ComponentScan("com.apiscope.flow")
public class AgenticDocsFlowAutoConfiguration {

    /**
     * Shared {@link ObjectMapper} injected into {@code FlowSseRegistry} and
     * {@code TraceSerializer}. Falls back to any bean already in the context.
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper flowObjectMapper() {
        return new ObjectMapper();
    }

    /**
     * Shared {@link RestClient} injected into {@code FlowExecutorService}.
     * Falls back to any bean already in the context.
     */
    @Bean
    @ConditionalOnMissingBean
    public RestClient flowRestClient() {
        return RestClient.create();
    }

    /**
     * Registers the Hibernate SQL interceptor only when Hibernate is on the classpath.
     * When the host application does not use JPA, this inner configuration is skipped entirely.
     */
    @Configuration
    @ConditionalOnClass(name = "org.hibernate.resource.jdbc.spi.StatementInspector")
    static class HibernateConfig {

        @Bean
        public FlowStatementInspector flowStatementInspector() {
            return new FlowStatementInspector();
        }

        /**
         * Registers the inspector as the Hibernate session-factory statement inspector.
         * Using a property customizer is the idiomatic Spring Boot way to configure Hibernate
         * without touching {@code application.properties}.
         */
        @Bean
        public HibernatePropertiesCustomizer flowHibernatePropertiesCustomizer(
                FlowStatementInspector inspector) {
            return props -> props.put(
                    "hibernate.session_factory.statement_inspector", inspector);
        }
    }
}


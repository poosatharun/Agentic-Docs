package com.agentic.docs.flow.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Spring Boot AutoConfiguration for the Agentic Docs Flow Tracer.
 *
 * <p>Activated only when:
 * <ul>
 *   <li>The application is a Servlet web application.</li>
 *   <li>{@code agentic.docs.flow.enabled=true} is set in {@code application.properties}.</li>
 * </ul>
 *
 * <p>{@code matchIfMissing=false} means Flow Tracer is <strong>OFF by default</strong>
 * in production — teams must explicitly opt in. This prevents AOP interception overhead
 * in environments where tracing is not needed.
 *
 * <p>To enable in development / sample app:
 * <pre>
 * # application.properties
 * agentic.docs.flow.enabled=true
 * </pre>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(
        prefix      = "agentic.docs.flow",
        name        = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
@EnableAspectJAutoProxy
@ComponentScan("com.agentic.docs.flow")
public class AgenticDocsFlowAutoConfiguration {
    // All beans registered via @ComponentScan.
    // FlowAspect, FlowSseRegistry, FlowExecutorService, FlowController
    // are discovered automatically from com.agentic.docs.flow.*
}

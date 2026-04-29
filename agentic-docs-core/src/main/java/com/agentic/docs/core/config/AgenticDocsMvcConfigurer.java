package com.agentic.docs.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * MVC configuration for Agentic Docs:
 * <ul>
 *   <li>Forwards {@code /}, {@code /agentic-docs}, and {@code /agentic-docs/}
 *       directly to the bundled {@code index.html}</li>
 *   <li>Registers global CORS rules for all {@code /agentic-docs/api/**} endpoints</li>
 * </ul>
 */
@Configuration
public class AgenticDocsMvcConfigurer implements WebMvcConfigurer {

    /**
     * Holds the CORS origins list from {@code agentic.docs.cors.allowed-origins}.
     * Injected by Spring via constructor injection.
     */
    private final AgenticDocsProperties properties;

    public AgenticDocsMvcConfigurer(AgenticDocsProperties properties) {
        this.properties = properties;
    }

    /**
     * Forward all UI entry-point URLs to the bundled React index.html.
     * Spring Boot only auto-serves index.html for the app root (/), NOT for
     * sub-paths like /agentic-docs/ — so we must forward explicitly.
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName(
                "forward:/agentic-docs/index.html");
        registry.addViewController("/agentic-docs").setViewName(
                "forward:/agentic-docs/index.html");
        registry.addViewController("/agentic-docs/").setViewName(
                "forward:/agentic-docs/index.html");
    }

    /**
     * Allow configured origins to call the Agentic Docs REST API.
     *
     * <p>Controlled via {@code agentic.docs.cors.allowed-origins} in
     * {@code application.properties}. Defaults to {@code http://localhost:5173}
     * (the Vite dev server) to avoid an open wildcard in production.</p>
     *
     * <p>Example — allow multiple origins:</p>
     * <pre>
     * agentic.docs.cors.allowed-origins=http://localhost:5173,https://myapp.com
     * </pre>
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Convert the List<String> to a plain String[] that Spring MVC expects
        List<String> origins = properties.cors().allowedOrigins();
        String[] originsArray = origins.toArray(new String[0]);

        registry.addMapping("/agentic-docs/api/**")
                .allowedOrigins(originsArray)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}

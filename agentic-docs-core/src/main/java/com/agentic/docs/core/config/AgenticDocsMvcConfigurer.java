package com.agentic.docs.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
     * Allow the Vite dev-server (port 5173) and any other origin to call
     * the Agentic Docs REST API, including preflight OPTIONS requests.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/agentic-docs/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}

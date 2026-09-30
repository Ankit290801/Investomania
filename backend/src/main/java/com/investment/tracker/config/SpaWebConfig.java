package com.investment.tracker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * E9 PWA Phase P0 — same-origin deployment prerequisite.
 *
 * Serves the built SPA from the backend so the app shell and /api share one
 * origin. This is what allows the PWA service worker to intercept and cache
 * API reads (a service worker only sees same-origin traffic), and it removes
 * CORS from the production story entirely. Development keeps the Vite dev
 * server + proxy (`/api` → :8080), which is unaffected by this config.
 *
 * Two behaviours:
 *  1. `/` and any non-API path without an extension resolve
 *     `classpath:/static/index.html` (SPA fallback — deep links like
 *     /investments/equity and PWA start_url work on hard refresh).
 *  2. Real files (hashed bundles, manifest.webmanifest, sw.js, icons) are
 *     served from static resources as-is.
 *
 * SecurityConfig already permits non-/api paths (`anyRequest().permitAll()`).
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    private static final String[] STATIC_EXTENSIONS = {
        ".html", ".js", ".css", ".svg", ".png", ".jpg", ".jpeg", ".gif",
        ".ico", ".webmanifest", ".json", ".txt", ".woff", ".woff2", ".map"
    };

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        // API and other non-SPA paths must 404, not fall back to the shell.
                        if (resourcePath.startsWith("api/")
                                || resourcePath.startsWith("actuator/")
                                || resourcePath.startsWith("h2-console/")) {
                            return null;
                        }
                        // SPA fallback for extension-less client routes.
                        if (isStaticAsset(resourcePath)) {
                            return null;
                        }
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }

    private boolean isStaticAsset(String path) {
        int queryIdx = path.indexOf('?');
        String clean = queryIdx >= 0 ? path.substring(0, queryIdx) : path;
        for (String ext : STATIC_EXTENSIONS) {
            if (clean.toLowerCase().endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}

package com.studyloop.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

class SpaFallbackResolverTest {

    @TempDir
    Path dir;

    private Resource resolve(String path) throws IOException {
        Files.writeString(dir.resolve("index.html"), "<!doctype html>");
        Files.createDirectories(dir.resolve("assets"));
        Files.writeString(dir.resolve("assets/app-1a2b.js"), "console.info(1)");
        Files.writeString(dir.resolve("favicon.svg"), "<svg/>");
        return new TestableResolver().resolve(path, new FileSystemResource(dir.toString() + "/"));
    }

    @Test
    void existingFilesAreServedAsIs() throws IOException {
        assertThat(resolve("assets/app-1a2b.js").getFilename()).isEqualTo("app-1a2b.js");
        assertThat(resolve("favicon.svg").getFilename()).isEqualTo("favicon.svg");
    }

    @Test
    void clientSideRoutesFallBackToIndexHtml() throws IOException {
        assertThat(resolve("app/doubts/42").getFilename()).isEqualTo("index.html");
        assertThat(resolve("").getFilename()).isEqualTo("index.html");
    }

    @Test
    void apiPathsAndMissingFilesAreNeverRewritten() throws IOException {
        assertThat(resolve("api/unknown")).isNull();
        assertThat(resolve("assets/missing-9z.js")).isNull();
    }

    private static final class TestableResolver {
        private final WebConfig.SpaFallbackResolver delegate = new WebConfig.SpaFallbackResolver();

        Resource resolve(String path, Resource location) throws IOException {
            java.lang.reflect.Method m;
            try {
                m = WebConfig.SpaFallbackResolver.class.getDeclaredMethod("getResource", String.class, Resource.class);
                m.setAccessible(true);
                return (Resource) m.invoke(delegate, path, location);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}

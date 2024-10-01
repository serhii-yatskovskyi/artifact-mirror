package org.bayaweaver.artifactmirror.codeartifact;

import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CodeartifactUrlFactoryTest {

    @Test
    public void general() {
        final URI originalUri = URI.create("/maven/my-repository/com/example/my-artifact/1.0/my-artifact-1.0.pom");
        HttpExchange e = mock(HttpExchange.class);
        when(e.getRequestURI()).thenReturn(originalUri);
        URL result = new CodeartifactUrlFactory("my-domain", "111122223333", "us-south-2").create(e);
        assertEquals(
                "https://my-domain-111122223333.d.codeartifact.us-south-2.amazonaws.com/maven/my-repository/com/example/my-artifact/1.0/my-artifact-1.0.pom",
                result.toString());
    }
}

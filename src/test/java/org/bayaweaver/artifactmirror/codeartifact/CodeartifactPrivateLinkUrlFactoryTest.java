package org.bayaweaver.artifactmirror.codeartifact;

import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CodeartifactPrivateLinkUrlFactoryTest {

    @Test
    public void general() {
        final URI originalUri = URI.create("/maven/my-repository/com/example/my-artifact/1.0/my-artifact-1.0.pom");
        final String privateLinkUrl = "https://vpce-1234-abcd.d.codeartifact.us-east-1.vpce.amazonaws.com/";
        HttpExchange e = mock(HttpExchange.class);
        when(e.getRequestURI()).thenReturn(originalUri);
        URL result = new CodeartifactPrivateLinkUrlFactory(privateLinkUrl, "my-domain", "111122223333").create(e);
        assertEquals(
                "https://vpce-1234-abcd.d.codeartifact.us-east-1.vpce.amazonaws.com/maven/d/my-domain-111122223333/my-repository/com/example/my-artifact/1.0/my-artifact-1.0.pom",
                result.toString());
    }

    @Test
    public void privateLinkWithoutTrailSlash() {
        final URI originalUri = URI.create("/maven/my-repository/com/example/my-artifact/1.0/my-artifact-1.0.pom");
        final String privateLinkUrl = "https://vpce-1234-abcd.d.codeartifact.us-east-1.vpce.amazonaws.com";
        HttpExchange e = mock(HttpExchange.class);
        when(e.getRequestURI()).thenReturn(originalUri);
        URL result = new CodeartifactPrivateLinkUrlFactory(privateLinkUrl, "my-domain", "111122223333").create(e);
        assertEquals(
                "https://vpce-1234-abcd.d.codeartifact.us-east-1.vpce.amazonaws.com/maven/d/my-domain-111122223333/my-repository/com/example/my-artifact/1.0/my-artifact-1.0.pom",
                result.toString());
    }
}

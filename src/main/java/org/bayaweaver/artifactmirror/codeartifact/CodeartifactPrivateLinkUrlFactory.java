package org.bayaweaver.artifactmirror.codeartifact;

import com.sun.net.httpserver.HttpExchange;
import org.bayaweaver.artifactmirror.ArtifactRepositoryUrlFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Creates URLS which follows the format
 * "https://vpce-1234-abcd.d.codeartifact.us-east-1.vpce.amazonaws.com/maven/d/my-domaub-111222333444/my-repository/..."
 */
public class CodeartifactPrivateLinkUrlFactory implements ArtifactRepositoryUrlFactory {
    private final URI privateLinkUrl;
    private final String domain;
    private final String domainOwner;

    public CodeartifactPrivateLinkUrlFactory(URI privateLinkUrl, String domain, String domainOwner) {
        this.privateLinkUrl = privateLinkUrl.toString().endsWith("/")
                ? privateLinkUrl
                : privateLinkUrl.resolve("/");
        if (!domain.matches("[a-z0-9.-]*")) {
            throw new IllegalArgumentException("Invalid CodeArtifact domain '" + domain + "'");
        }
        if (!domainOwner.matches("[0-9]*")) {
            throw new IllegalArgumentException("Invalid CodeArtifact domain owner (account ID) '" + domainOwner + "'");
        }
        this.domain = domain;
        this.domainOwner = domainOwner;
    }

    @Override
    public URL create(HttpExchange exchange) {
        String resourcePath = exchange.getRequestURI().toString();
        int n = resourcePath.indexOf('/', 1);
        String format = resourcePath.substring(0, n).replace("/", "");
        resourcePath = resourcePath.substring(n + 1);
        n = resourcePath.indexOf('/');
        String repository = resourcePath.substring(0, n);
        resourcePath = resourcePath.substring(n + 1);
        URI uri = privateLinkUrl.resolve(
                format + "/d/" + domain + "-" + domainOwner + "/" + repository + "/" + resourcePath);
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}

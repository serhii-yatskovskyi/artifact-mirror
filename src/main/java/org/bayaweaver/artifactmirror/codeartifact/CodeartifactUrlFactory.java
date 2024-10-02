package org.bayaweaver.artifactmirror.codeartifact;

import com.sun.net.httpserver.HttpExchange;
import org.bayaweaver.artifactmirror.ArtifactRepositoryUrlFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Creates URLS which follows the format
 * "https://my-domain-111122223333.d.codeartifact.us-east-1.amazonaws.com/maven/my-repository/..."
 */
public class CodeartifactUrlFactory implements ArtifactRepositoryUrlFactory {
    private final String repositoryUrl;

    public CodeartifactUrlFactory(
            String codeartifactDomain,
            String codeartifactDomainOwner,
            String codeartifactRegion) {

        try {
            this.repositoryUrl = URI
                    .create("https://" + codeartifactDomain + "-" + codeartifactDomainOwner
                            + ".d.codeartifact." + codeartifactRegion + ".amazonaws.com")
                    .toURL()
                    .toString();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public URL create(HttpExchange exchange) {
        try {
            return URI.create(repositoryUrl + exchange.getRequestURI()).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}

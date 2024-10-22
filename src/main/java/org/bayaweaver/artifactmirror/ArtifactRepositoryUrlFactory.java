package org.bayaweaver.artifactmirror;

import com.sun.net.httpserver.HttpExchange;

import java.net.URL;

public interface ArtifactRepositoryUrlFactory {

    URL create(HttpExchange exchange);
    URL repositoryUrl();
}

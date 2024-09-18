package org.bayaweaver.artifactmirror;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Application {

    public static void main(String[] args) throws IOException {
        ApplicationOptions opts = ApplicationOptions.parse(args);
        HttpHandler httpRequestHandler = ArtifactRepositoryRequestHandler.codeartifact(
                opts.value(ApplicationOptions.Option.CODEARTIFACT_DOMAIN),
                opts.value(ApplicationOptions.Option.CODEARTIFACT_DOMAIN_OWNER),
                opts.value(ApplicationOptions.Option.CODEARTIFACT_REGION),
                opts.value(ApplicationOptions.Option.AWS_ACCESS_KEY_ID),
                opts.value(ApplicationOptions.Option.AWS_SECRET_ACCESS_KEY));
        int port = Integer.parseInt(opts.value(ApplicationOptions.Option.HTTP_SERVER_PORT));
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/", httpRequestHandler);
        httpServer.start();
        System.out.println("Server is listening " + port + " HTTP port");
    }
}

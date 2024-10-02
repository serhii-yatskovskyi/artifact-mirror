package org.bayaweaver.artifactmirror;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bayaweaver.artifactmirror.codeartifact.CodeartifactAuthorizationTokenProvider;
import org.bayaweaver.artifactmirror.codeartifact.CodeartifactPrivateLinkUrlFactory;
import org.bayaweaver.artifactmirror.codeartifact.CodeartifactUrlFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;

public class Application {

    public static void main(String[] args) throws IOException {
        final int port;
        final HttpHandler httpRequestHandler;
        try {
            Options opts = Options.parse(args);
            ArtifactRepositoryUrlFactory artifactRepositoryUrlFactory;
            if (opts.contains(Option.CODEARTIFACT_ENDPOINT)) {
                artifactRepositoryUrlFactory = new CodeartifactPrivateLinkUrlFactory(
                        URI.create(opts.value(Option.CODEARTIFACT_ENDPOINT)),
                        opts.value(Option.CODEARTIFACT_DOMAIN),
                        opts.value(Option.CODEARTIFACT_DOMAIN_OWNER));
            } else {
                artifactRepositoryUrlFactory = new CodeartifactUrlFactory(
                        opts.value(Option.CODEARTIFACT_DOMAIN),
                        opts.value(Option.CODEARTIFACT_DOMAIN_OWNER),
                        opts.value(Option.CODEARTIFACT_REGION));
            }
            URI codeartifactApiEndpoint = null;
            if (opts.contains(Option.CODEARTIFACT_API_ENDPOINT)) {
                if (opts.contains(Option.CODEARTIFACT_ENDPOINT)) {
                    codeartifactApiEndpoint = URI.create(opts.value(Option.CODEARTIFACT_API_ENDPOINT));
                } else {
                    System.out.println(Color.yellow("WARNING: '--" + Option.CODEARTIFACT_API_ENDPOINT
                            + "' requires '--" + Option.CODEARTIFACT_ENDPOINT
                            + "' presence. The value will be ignored"));
                }
            }
            AuthorizationTokenProvider authorizationTokenProvider = new CodeartifactAuthorizationTokenProvider(
                    opts.value(Option.CODEARTIFACT_DOMAIN),
                    opts.value(Option.CODEARTIFACT_DOMAIN_OWNER),
                    opts.value(Option.CODEARTIFACT_REGION),
                    opts.value(Option.AWS_ACCESS_KEY_ID),
                    opts.value(Option.AWS_SECRET_ACCESS_KEY),
                    codeartifactApiEndpoint);
            httpRequestHandler = new ArtifactRepositoryRequestHandler(
                    artifactRepositoryUrlFactory,
                    authorizationTokenProvider);
            port = Integer.parseInt(opts.value(Option.SERVER_PORT));
        } catch (Exception e) {
            System.err.println(Color.red("ERROR: " + e.getMessage()));
            System.exit(1);
            return;
        }
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/", httpRequestHandler);
        httpServer.start();
        System.out.println(Color.green("Server is listening " + port + " HTTP port"));
    }

    private static class Color {
        private static final String RED = "\u001B[31m";
        private static final String GREEN = "\u001B[32m";
        private static final String RESET = "\u001B[0m";
        private static final String YELLOW = "\u001B[33m";

        static String red(String src) {
            return RED + src + RESET;
        }

        static String green(String src) {
            return GREEN + src + RESET;
        }

        static String yellow(String src) {
            return YELLOW + src + RESET;
        }
    }
}

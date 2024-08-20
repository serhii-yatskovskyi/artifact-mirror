# CodeArtifact Proxy

## Description

Access to CodeArtifact is restricted without an option to disable such behavior, so developers must configure Maven to
use an authorization mechanism. Since the CodeArtifact token expires every 12 hours, Maven settings need to be refreshed
regularly to maintain access. The CodeArtifact Proxy is designed to act as a mediator between artifact consumers (a
developer's local machine, BitBucket, etc.) and CodeArtifact, routing all requests to the service and equipping them
with an authorization token.

## Running Jar-File

All options mentioned below are required for the successful run.

```shell
java -jar codeartifact-proxy-1.0.4.jar\
 --aws.codeartifact.domain=...\
 --aws.codeartifact.domain-owner=...\
 --aws.codeartifact.region=...\
 --aws.access-key-id=...\
 --aws.secret-access-key=...\
 --server.ssl.key-store=<key-store-file-path>\
 --server.ssl.key-store-password=...\
 --server.ssl.key-store-alias=...
``` 

This command runs the application on the port 443. To change the port, perform the command above with the parameter
`--server.port`.

After this, the CodeArtifact Proxy can be considered as the CodeArtifact repository, and artifacts can be requested
through it in a normal way without the authentication need. To instruct Maven to access artifacts from the applicaion,
add or replace the `<repository>` section in the `pom.xml`:

```xml
<project>
    ...
    <repositories>
        <repository>
            <id>bnc-obs-release</id>
            <!-- The application has been run with the parameters `aws.codeartifact.domain=my-domain`,
            `aws.codeartifact.domain-owner=111222333444`, and `aws.codeartifact.region=us-east-1` -->
            <!-- A previous value was
            https://my-domain-111222333444.d.codeartifact.us-east-1.amazonaws.com/maven/release/ -->
            <url>https://<codeartifact-proxy-address>/maven/bnc-obs-release/</url>
        </repository>
    </repositories>
</project>
```

The value of the `<codeartifact-proxy-address>` parameter depends on what key store file (or more precisely, what SSL
certificate within it) is used. During execution, Maven requires connection through HTTPS and a trusted SSL certificate,
otherwise it immediately stops execution by an error. To avoid this, one of the solutions could be to **obtain the SSL
certificate from a trusted CA**. In this case, the `<codeartifact-proxy-address>` is replaced by a domain name for which
the certificate was issued, and, in addition, this domain name must be mapped on a certain IP address in the
`C:/Windows/system32/drivers/etc/host` file. For instance, if the CodeArtifact Proxy is deployed on your local machine,
the record below must be added to the file:

```
127.0.0.1   <ssl-certificate-domain>
```

If the CodeArtifact Proxy is deployed on a server within a private network which is accessible through VPN, the
`127.0.0.1` must be replaced with the private IP address of this server.

In case **the SSL certificate is a self-signed**, it must be added to default key stores of all JRE which are installed
on your local machine, and the `<codeartifact-proxy-address>` takes an actual IP address of the server with the deployed
application.

# CodeArtifact Proxy

## Description

Access to CodeArtifact is restricted without an option to disable such behavior, so developers must configure Maven to
use an authorization mechanism. Since the CodeArtifact token expires every 12 hours, Maven settings need to be refreshed
regularly to maintain access. The CodeArtifact Proxy is designed to act as a mediator between artifact consumers (a
developer's local machine, BitBucket, etc.) and CodeArtifact, routing all requests to the service and equipping them
with an authorization token.

## Running Jar-File

```shell
java -jar codeartifact-proxy-1.0.4.jar --aws.codeartifact.domain=? --aws.codeartifact.domain-owner=? --aws.codeartifact.region=? --aws.access-key-id=? --aws.secret-access-key=?
```

This command runs the Proxy on the port 443. To change the port, run the Proxy with the parameter `--server.port`.

After this, CodeArtifact Proxy can be considered as the CodeArtifact repository, and artifacts can be requested through
it in a normal way without the authentication need. In case your project depends on artifacts from the CodeArtifact
repository, add or replace the `<repository>` section in the `pom.xml`:

```xml
<project>
    ...
    <repositories>
        <repository>
            <id>bnc-obs-release</id>
            <!--The address below was specified before; the CodeArtifact Proxy is being run on a local machine with
            the parameters 'bnc-obs', '435280699592' and 'us-east-2'-->
            <!--url>https://bnc-obs-435280699592.d.codeartifact.us-east-2.amazonaws.com/maven/bnc-obs-release/</url-->
            <url>https://codeartifact.private.bayaweaver.org/maven/bnc-obs-release/</url>
        </repository>
    </repositories>
</project>
```

Then, add a record to the `C://Windows/system32/drivers/etc/host` file:

```
127.0.0.1   codeartifact.private.bayaweaver.org
```

CodeArtifact Proxy can be deployed on a remote server instance; in this case, in the `host` file, instead of
`127.0.0.1`, specify the remote server's IP address.

## Alternative SSL Certificate Usage

The `codeartifact.private.bayaweaver.org` domain must be used due to a built-in certificate is registered in the CA
under this domain. Instead of using the built-in certificate, an external can be used:

```
java -jar codeartifact-proxy-1.0.4.jar --server.ssl.key-store=C:\\keystore.p12 --server.ssl.key-store-password=1234 --server.ssl.key-store-alias=<alias> ... <other parameters>
```

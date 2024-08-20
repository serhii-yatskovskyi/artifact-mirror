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

> **Warning!** An SSL certificate in the key store must be issued by any trusted CA, i.e. not be self-signed. In case
> the certificate is self-signed, Maven is not able to verify it and immediately stops execution by an error.  

This command runs the application on the port 443. To change the port, perform the command above with the parameter
`--server.port`.

After this, the application can be considered as the CodeArtifact repository, and artifacts can be requested through
it in a normal way without the authentication need. To instruct Maven to access artifacts through this repository,
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
            <url>https://<ssl-certificate-domain>/maven/bnc-obs-release/</url>
        </repository>
    </repositories>
</project>
```

The `<ssl-certificate-domain>` value is the domain for which an SSL certificate was issued by CA. 

Then, add a record to the `C:/Windows/system32/drivers/etc/host` file:

```
<application-server-address>   <ssl-certificate-domain>
```

In case of local run, the record must look like

```
127.0.0.1   <ssl-certificate-domain>
```
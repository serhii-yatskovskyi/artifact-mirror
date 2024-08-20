# CodeArtifact Proxy

## Description

Access to CodeArtifact is restricted without an option to disable such behavior, so developers must configure Maven to
use an authorization mechanism. Since the CodeArtifact token expires every 12 hours, Maven settings need to be refreshed
regularly to maintain access. The CodeArtifact Proxy is designed to act as a mediator between artifact consumers (a
developer's local machine, BitBucket, etc.) and CodeArtifact, routing all requests to the service and equipping them
with an authorization token.

## Running Jar-File

The following options are required for successful execution:

```shell
java -jar codeartifact-proxy-1.0.4.jar\
 --aws.codeartifact.domain=...\
 --aws.codeartifact.domain-owner=...\
 --aws.codeartifact.region=...\
 --aws.access-key-id=...\
 --aws.secret-access-key=...\
 --server.ssl.certificate=~/certificate.crt\
 --server.ssl.private-key=~/private.key
``` 

This command runs the application on the port 443. To change the port, execute the command above with the parameter
`--server.port`.

A root certificate can be added using the optional `--server.ssl.ca_bundle` parameter.

Once the CodeArtifact Proxy is running, you can configure Maven to access artifacts through it in a normal way without
the authentication need. To do this, replace or add the following `<repository>` section in your `pom.xml`:

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
            <url>https://<codeartifact-proxy-address>/maven/bnc-obs-release/
            </url>
        </repository>
    </repositories>
</project>
```

The value of the `<codeartifact-proxy-address>` parameter depends on what SSL certificate is used. During execution,
Maven requires: 1) connection through HTTPS and 2) a trusted SSL certificate, - otherwise it immediately stops execution
by an error. To avoid this, either the certificate must be issued by a trusted CA or a self-signed certificate must be
added to the Java Key Store. There are multiple actual solutions, each of them depends on a network architecture and has
it's proc and cons.

The value of the `<codeartifact-proxy-address>` parameter depends on the SSL certificate used. Maven requires an HTTPS
connection and a trusted SSL certificate; otherwise, it terminates the connection with an error. To avoid this, either
the certificate must be issued by a trusted CA or a self-signed certificate must be
added to the Java Key Store. The specific solution depends on your network architecture and has its own pros and cons.

### CodeArtifact Proxy as a Server in a Private Network Accessible Through VPN

If the VPN server is available to be configured, you can set it to route all DNS traffic through it. In the private
network, a private DNS server must be deployed. This DNS server should have an A-record that maps the SSL-certificate
domain to the private IP address of the CodeArtifact Proxy server.

If VPN configuration is not possible, you need to add a record to the `C:/Windows/system32/drivers/etc/hosts`
file on each local machine connected to the VPN:

```
192.168.1.5   <ssl-certificate-domain>
```

where `192.168.1.5` is the IP address of the CodeArtifact Proxy server. Then, replace `<codeartifact-proxy-address>` in
the `<repository>` section of your `pom.xml` with the value of `ssl-certificate-domain`.

### Self-Signed Certificates

If you are using a self-signed SSL certificate, you must add it to the default key stores of all JREs installed on your
local machines. In this case, the `<codeartifact-proxy-address>` in the `<repository>` section of the `pom-xml` must
be the actual IP address of the CodeArtifact Proxy server.

package org.bayaweaver.artifactmirror;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OptionsTest {

    @Test
    public void requiredOptionsPresent() {
        assertDoesNotThrow(() -> {
            Options.parse(new String[]{
                    "--aws.codeartifact.domain=my-domain",
                    "--aws.codeartifact.domainOwner=111122223333",
                    "--aws.codeartifact.region=Z"});
        });
    }

    @Test
    public void requiredOptionsBlank() {
        assertThrows(IllegalArgumentException.class, () -> {
            Options.parse(new String[]{
                    "--aws.codeartifact.domain",
                    "--aws.codeartifact.domainOwner=111122223333",
                    "--aws.codeartifact.region=Z"});
        });
        assertThrows(IllegalArgumentException.class, () -> {
            Options.parse(new String[]{
                    "--aws.codeartifact.domain=my-domain",
                    "--aws.codeartifact.domainOwner",
                    "--aws.codeartifact.region=Z"});
        });
        assertThrows(IllegalArgumentException.class, () -> {
            Options.parse(new String[]{
                    "--aws.codeartifact.domain=my-domain",
                    "--aws.codeartifact.domainOwner=111122223333",
                    "--aws.codeartifact.region"});
        });
    }

    @Test
    public void requiredOptionsAbsent() {
        assertThrows(IllegalArgumentException.class, () -> {
            Options.parse(new String[]{
                    "--aws.codeartifact.domainOwner=111122223333",
                    "--aws.codeartifact.region=Z"});
        });
        assertThrows(IllegalArgumentException.class, () -> {
            Options.parse(new String[]{
                    "--aws.codeartifact.domain=my-domain",
                    "--aws.codeartifact.region=Z"});
        });
        assertThrows(IllegalArgumentException.class, () -> {
            Options.parse(new String[]{
                    "--aws.codeartifact.domain=my-domain",
                    "--aws.codeartifact.domainOwner=111122223333"});
        });
    }

    @Test
    public void defaultValues() {
        Options opts = Options.parse(new String[]{
                "--aws.codeartifact.domain=my-domain",
                "--aws.codeartifact.domainOwner=111122223333",
                "--aws.codeartifact.region=Z"});
        assertEquals("80", opts.value(Option.SERVER_PORT));
        assertFalse(opts.contains(Option.AWS_ACCESS_KEY_ID));
        assertFalse(opts.contains(Option.AWS_SECRET_ACCESS_KEY));
        assertFalse(opts.contains(Option.CODEARTIFACT_ENDPOINT));
        assertFalse(opts.contains(Option.CODEARTIFACT_API_ENDPOINT));
    }

    @Test
    public void requiredPairsPresent() {
        assertDoesNotThrow(() -> {
            Options.parse(new String[]{
                    "--aws.codeartifact.domain=my-domain",
                    "--aws.codeartifact.domainOwner=111122223333",
                    "--aws.codeartifact.region=Z",
                    "--aws.codeartifact.vpc-endpoint=http://a",
                    "--aws.codeartifact.api.vpc-endpoint=http://b"});
        });
    }

    @Test
    public void requiredPairsAbsent() {
        assertThrows(IllegalArgumentException.class, () -> {
            Options.parse(new String[]{
                    "--aws.codeartifact.domain=my-domain",
                    "--aws.codeartifact.domainOwner=111122223333",
                    "--aws.codeartifact.region=Z",
                    "--aws.codeartifact.vpc-endpoint=http://a"});
        });
        assertDoesNotThrow(() -> {
            Options.parse(new String[]{
                    "--aws.codeartifact.domain=my-domain",
                    "--aws.codeartifact.domainOwner=111122223333",
                    "--aws.codeartifact.region=Z",
                    "--aws.codeartifact.api.vpc-endpoint=http://b"});
        });
        assertThrows(IllegalArgumentException.class, () -> {
            Options.parse(new String[]{
                    "--aws.codeartifact.domain=my-domain",
                    "--aws.codeartifact.domainOwner=111122223333",
                    "--aws.codeartifact.region=Z",
                    "--aws.accessKeyId=C"});
        });
        assertThrows(IllegalArgumentException.class, () -> {
            Options.parse(new String[]{
                    "--aws.codeartifact.domain=my-domain",
                    "--aws.codeartifact.domainOwner=111122223333",
                    "--aws.codeartifact.region=Z",
                    "--aws.secretKey=D"});
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "UPPERCASE", "invalid_domain", "domain@"})
    public void invalidDomainValues(String domain) {
        assertThrows(IllegalArgumentException.class, () -> {
            Options.parse(new String[]{
                    "--aws.codeartifact.domain=" + domain,
                    "--aws.codeartifact.domainOwner=111122223333",
                    "--aws.codeartifact.region=Z"});
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "123", "11112222333344445555", ""})
    public void invalidDomainOwnerValues(String domainOwner) {
        assertThrows(IllegalArgumentException.class, () -> {
            Options.parse(new String[]{
                    "--aws.codeartifact.domain=my-domain",
                    "--aws.codeartifact.domainOwner=" + domainOwner,
                    "--aws.codeartifact.region=Z"});
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"s3://a", "a", ""})
    public void invalidVpcEndpoint(String endpoint) {
        assertThrows(IllegalArgumentException.class, () -> {
            Options.parse(new String[]{
                    "--aws.codeartifact.domain=my-domain",
                    "--aws.codeartifact.domainOwner=111122223333",
                    "--aws.codeartifact.region=Z",
                    "--aws.codeartifact.vpc-endpoint=" + endpoint,
                    "--aws.codeartifact.api.vpc-endpoint=http://b"});
        });
        assertThrows(IllegalArgumentException.class, () -> {
            Options.parse(new String[]{
                    "--aws.codeartifact.domain=my-domain",
                    "--aws.codeartifact.domainOwner=111122223333",
                    "--aws.codeartifact.region=Z",
                    "--aws.codeartifact.vpc-endpoint=http://a",
                    "--aws.codeartifact.api.vpc-endpoint=" + endpoint});
        });
    }
}

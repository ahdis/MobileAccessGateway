# 8. Configuration

This page details how to configure the MobileAccessGateway application.

## Publish as TCU without XUA token

The MAG can automatically inject a technical user (TCU) XUA token into ITI-65 requests when no `Authorization` 
header is present. To do that, the following configuration parameters must be set:

```yaml
mag:
    auth:
        sts: http://${host}/STS/services/SecurityTokenService
        sts-issuer: https://test.ahdis.ch/mag-cara
        tcu:
            principal-name: Gregory House
            principal-gln: 2000040030826
            oid: 2.16.756.5.30.1.999.9.1.1.1
            keystore-path: file:secret/tcu.p12
            keystore-password: MySecretPassword
            keystore-alias: mykeyalias
            auto-inject-in-iti65: true
```

Details of the parameters:

- `mag.auth.sts`: The URL of the STS service (for the Get X-User Assertion request).
- `mag.auth.sts-issuer`: The `Assertion/Issuer` value of the TCU SAML Assertion.
- `mag.auth.tcu.principal-name`: The `Subject/NameID` value of the TCU SAML Assertion.
- `mag.auth.tcu.principal-gln`: The `Subject/SubjectConfirmation/Data/Recipient` value of the TCU SAML Assertion.
- `mag.auth.tcu.oid`: The OID of the TCU, as registered in the EPR community.
- `mag.auth.tcu.keystore-path`: The path to the keystore containing the private key to sign the SAML Assertion. It 
  should begin with `file:` and be a PKCS#12 file (`.p12` or `.pfx`).
- `mag.auth.tcu.keystore-password`: The password of the keystore.
- `mag.auth.tcu.keystore-alias`: The alias of the private key in the keystore.
- `mag.auth.tcu.auto-inject-in-iti65`: Set to `true` to enable the automatic injection of the TCU token in ITI-65 
  requests.

## Create a PKCS#12 keystore

You need a PKCS#12 keystore for using mTLS with the EPR service, and for signing the TCU SAML Assertion (if you 
enabled that option).
To create a PKCS#12 keystore, you need a private key and a certificate that you may have generated yourself, or that 
you received from a certificate authority/EPR community.
They will usually be in the PEM format (files usually ending with `.pem`, `.crt`, `.cer` or `.key`); the certificate 
should start with `-----BEGIN CERTIFICATE-----` and the private key with `-----BEGIN PRIVATE KEY-----`,
`BEGIN ENCRYPTED PRIVATE KEY` or `BEGIN RSA PRIVATE KEY`.

In a terminal with [OpenSSL](https://www.openssl.org/):
1. To create a new keystore:
   `openssl pkcs12 -export -in cert.pem -inkey private_pkcs8.pem -out keystore.p12 -name mykeyalias`. OpenSSL will take
   care of any type of private key (PKCS#1, PKCS#8, encrypted or not).
2. Optional: fill the private key password (_Enter pass phrase for private.pem_).
3. Fill the keystore password (_Enter Export Password_).

To add one key pair from one PKCS#12 keystore to another PKCS#12 keystore:
1. `keytool -importkeystore -deststorepass:file destKeyStorePasswordFile -destkeystore destination.p12 -srckeystore source.p12 -deststoretype PKCS12 -srcstoretype PKCS12 -srcstorepass:file myOutPasswordFile -alias mykeyalias`

In a GUI with [KeyStore Explorer](https://keystore-explorer.org/):
1. Open KeyStore Explorer, click on `Create a new KeyStore` and choose `PKCS#12`, or open an existing PKCS#12 keystore.
2. Right-click in the empty area and choose `Import Key Pair`.
3. Select `Import from PEM encoded files`.
4. If your private key had `RSA PRIVATE KEY`, select `OpenSSL`. Otherwise (`PRIVATE KEY` and `ENCRYPTED PRIVATE KEY`),
   select `PKCS#8`.
5. Select your private key file and your certificate file; specify the password of the private key if needed.
6. Enter an alias for the key pair. You will fill it in the configuration properties `mag.auth.tcu.keystore-alias` or 
   `mag.client-ssl.cert-alias`.
7. Leave the key pair password fields empty.
8. Save your keystore.

It should look like:
[![KeyStore Explorer screenshot](./assets/keystore_result.png)](./assets/keystore_result.png)

## Create a PKCS#12 truststore

You need a PKCS#12 truststore for using mTLS with the EPR service. To create a PKCS#12 truststore, you need the 
certificate(s) to trust. – it can be the leaf certificate, any intermediate certificate(s) and/or the CA certificate.
The choice of the certificate to trust is yours:

1. The leaf certificate is the safer to trust, because only the owner of the private key of that certificate will be 
   trusted. But when the leaf certificate is renewed (usually every year), you will need to update your truststore. 
   The MAG won't work anymore until you do that, so it is not very convenient.
2. The intermediate certificate(s) or the CA certificate are more convenient, because they usually have a longer validity 
   (many years). But they are less safe, because any leaf certificate issued by that CA will be trusted – although 
   CAs trusted by your browser should be very strict when issuing a certificate for a given domain.

When finding the certificate(s) to trust, both OpenSSL and your browser will show you the validity period of each 
certificate in the chain.

To find the certificates of a TLS host:

In a terminal with [OpenSSL](https://www.openssl.org/): `openssl s_client -connect www.epr.ch:443 -showcerts`.
You will find the leaf, intermediate and root certificates in the output, each starting with 
`-----BEGIN CERTIFICATE-----` and ending with `-----END CERTIFICATE-----`. Copy the certificate you want, including 
the boundaries (`-----BEGIN CERTIFICATE-----` and `-----END CERTIFICATE-----`), into a `.pem` file.

In a GUI with Chrome:
1. Click on the _View site information_ icon on the left of the address bar:
   [![KeyStore Explorer screenshot](./assets/chrome_info_button.png)](./assets/chrome_info_button.png)
2. Click on _Connection is secure_.
3. Click on _Certificate is valid_.
4. In the _Details_ tab, select the certificate you want to trust. At the top is the CA, at the bottom is the
   leaf certificate.
5. Click on the _Export_ button (bottom right) and save the certificate in the `.pem` format.

Then, add it to a PKCS#12 truststore – it can be the same file as the keystore.

In a terminal with OpenSSL to create a new keystore:
`openssl pkcs12 -export -in trusted_cert.pem -nokeys -out truststore.p12 -name trustalias`

In a terminal with keytool to add one certificate to an existing keystore:
`keytool -import -trustcacerts -keystore existing.p12 -storetype PKCS12 -storepass yourKeystorePassword -alias trustalias -file trusted_cert.pem`

In a GUI with KeyStore Explorer:
1. Open KeyStore Explorer, click on `Create a new KeyStore` and choose `PKCS#12`, or open an existing PKCS#12 keystore.
2. Right-click in the empty area and choose `Import Trusted Certificate`.
3. Select the certificate file you created above.
4. Enter an alias for the certificate. This alias won't be used in the configuration, so choose an alias that helps you 
   remember what certificate it is.
5. Save your truststore.

It should look like:
[![KeyStore Explorer screenshot](./assets/truststore_result.png)](./assets/truststore_result.png)
# 7. Changelog

## v2.0.5 - Unreleased

- Inject a random traceparent header in the outgoing requests if it's not provided in the request.
- Improve the logging in the application, especially at the DEBUG and TRACE levels.
- Update dependencies.

## v2.0.4 - 2025/11/03

- Fix the ATNA TLS initialization (#37).

## v2.0.3 - 2025/10/08

- Add STS/TCU support for emedo (#24).
- Update Postman collection for emedo.
- Remove the configuration parameter `mag.auth.sts-issuer` (#27).
- Remove the configuration parameter `mag.auth.tcu.template-path` (#26).
- Add the configuration parameter `mag.auth.tcu.oid` (#26).
- Add author support in publication (#31).
- Implement the automatic loop on confidentiality codes when publishing a document (#22).

## v2.0.2 - 2025/10/01

- Enable https with client certificates and without (#19).
- Fix startup issue when XCA endpoints are not defined (#18). 

## v2.0.1 - 2025/09/30

- Simplify the patient identifier feed (#15).
  The `Patient.managingOrganization` isn't needed anymore.
- Create the configuration parameter `mag.organization-name`.
- Fix the URN-encoding of `DocumentEntry.entryUUID` (#16).
- Don't return an OperationOutcome in ITI-119 when there are no matches (#17).

## v2.0.0 - 2025/09/26

- Initial release of this MobileAccessGateway instance for integration to the Swiss EPR.
- The application is running on Java 25, Jetty 12.0.25, IPF 5.1.0, Spring Boot 3.5.5, FHIR Core 6.5.18 and HAPI FHIR
  8.2.1.
- This version has been tested at the Digital Health Projectathon 2025 in Bern.

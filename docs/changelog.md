# 7. Changelog

## v2.0.3 - unreleased

- Add STS/TCU support for emedo [#24](https://github.com/ahdis/MobileAccessGateway/issues/24)).
- Update Postman collection for emedo.
- Remove the configuration parameter `mag.auth.sts-issuer` (#27).

## v2.0.2 - 2025/10/01

- Enable https with client certificates and without ([#19](https://github.com/ahdis/MobileAccessGateway/issues/19)).
- Fix startup issue when XCA endpoints are not defined ([#18](https://github.com/ahdis/MobileAccessGateway/issues/18)). 

## v2.0.1 - 2025/09/30

- Simplify the patient identifier feed ([#15](https://github.com/ahdis/MobileAccessGateway/issues/15)).
  The `Patient.managingOrganization` isn't needed anymore.
- Create the configuration parameter `mag.organization-name`.
- Fix the URN-encoding of `DocumentEntry.entryUUID` ([#16](https://github.com/ahdis/MobileAccessGateway/issues/16)).
- Don't return an OperationOutcome in ITI-119 when there are no matches
  ([#17](https://github.com/ahdis/MobileAccessGateway/issues/17)).

## v2.0.0 - 2025/09/26

- Initial release of this MobileAccessGateway instance for integration to the Swiss EPR.
- The application is running on Java 25, Jetty 12.0.25, IPF 5.1.0, Spring Boot 3.5.5, FHIR Core 6.5.18 and HAPI FHIR
  8.2.1.
- This version has been tested at the Digital Health Projectathon 2025 in Bern.

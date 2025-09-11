# 2. Features

## 2.1 IHE Transactions & Actors

The Mobile Access Gateway (MAG) provides translation between REST/FHIR requests defined in the [CH EPR FHIR IG](https://fhir.ch/ig/ch-epr-fhir/index.html) and SOAP/HL7v3 variants of IHE transactions.    

The table below lists the supported IHE profiles, their transactions and the involved IHE actors implemented in the MAG. 

| REST/FHIR |   |   | SOAP/HL7v3 |   |   |
|---|---|---|---|---|---|
| **Profile** | **Transaction** | **Actor** | **-> Actor** | **Transaction** | **Profile** | 
| [PDQm](https://fhir.ch/ig/ch-epr-fhir/iti-pdqm.html) | [Patient Demographics Match [ITI-119]](https://fhir.ch/ig/ch-epr-fhir/iti-119.html) | Patient Demographics Supplier | -> Patient Demographics Consumer | Patient Demographics Query HL7 V3 [ITI-47] | PDQV3 |
| [PIXm](https://fhir.ch/ig/ch-epr-fhir/iti-pixm.html) | [Mobile Patient Identifier Cross-reference Query [ITI-83]](https://fhir.ch/ig/ch-epr-fhir/iti-83.html) | Patient Identity Manager | -> Patient Identifier Cross-reference Consumer | PIXV3 Query [ITI-45] | PIXV3 |
| [PIXm](https://fhir.ch/ig/ch-epr-fhir/iti-pixm.html) | [Patient Identity Feed FHIR [ITI-104]](https://fhir.ch/ig/ch-epr-fhir/iti-104.html) | Patient Identity Manager | -> Patient Identity Source | Patient Identity Feed HL7 V3 [ITI-44] | PIXV3 |
| [MHD](https://fhir.ch/ig/ch-epr-fhir/iti-mhd.html) | [Provide Document Bundle [ITI-65]](https://fhir.ch/ig/ch-epr-fhir/iti-65.html) | Document Recipient | -> Document Source, X-Service-User | Provide and Register Document Set-b [ITI-41] | XDS.b |
| [MHD](https://fhir.ch/ig/ch-epr-fhir/iti-mhd.html) | [Update Document Metadata [CH:MHD-1]](https://fhir.ch/ig/ch-epr-fhir/ch-mhd-1.html) | Document Responder | -> Document Administrator | Update Document Set [ITI-57] | XDS.b |
| [MHD](https://fhir.ch/ig/ch-epr-fhir/iti-mhd.html) | [Find Document References [ITI-67]](https://fhir.ch/ig/ch-epr-fhir/iti-67.html) | Document Responder | -> Document Consumer, X-Service-User | Registry Stored Query [ITI-18] | XDS.b |
| [MHD](https://fhir.ch/ig/ch-epr-fhir/iti-mhd.html) | [Retrieve Document [ITI-68]](https://fhir.ch/ig/ch-epr-fhir/iti-68.html) | Document Responder | -> Document Consumer, X-Service-User | Retrieve Document Set [ITI-43] | XDS.b |
| [CH:PPQm](https://fhir.ch/ig/ch-epr-fhir/ppqm.html) | [Mobile Privacy Policy Feed [CH:PPQ-3]](https://fhir.ch/ig/ch-epr-fhir/ppq-3.html) | Policy Repository | -> Policy Source | Privacy Policy Feed [CH:PPQ‑1] / Privacy Policy Retrieve [CH:PPQ‑2] | CH:PPQ |
| [CH:PPQm](https://fhir.ch/ig/ch-epr-fhir/ppqm.html) | [Mobile Privacy Policy Bundle Feed [CH:PPQ-4]](https://fhir.ch/ig/ch-epr-fhir/ppq-4.html) | Policy Repository | -> Policy Source | Privacy Policy Feed [CH:PPQ‑1] / Privacy Policy Retrieve [CH:PPQ‑2] | CH:PPQ |
| [CH:PPQm](https://fhir.ch/ig/ch-epr-fhir/ppqm.html) | [Mobile Privacy Policy Retrieve [CH:PPQ-5]](https://fhir.ch/ig/ch-epr-fhir/ppq-5.html) | Policy Repository | -> Policy Source | Privacy Policy Retrieve [CH:PPQ‑2] | CH:PPQ |

### 2.1.1 Authentication/Authorization

1. PDQm and PIXm transactions don't require authentication.
2. ITI-65 requests can be automatically authorized as the configured Technical User (TCU).
3. Other MHD transactions require authorization.

## 2.2 CH EPR Constraints

The Swiss EPR defines constraints on the use of MHD, PIXm, PDQm and PPQm transactions. Those can be enabled in the 
configuration file:
```yml
mag:
  ch-epr-fhir:
    ch-mhd-constraints: true
    ch-ppqm-constraints: true
    ch-pixm-constraints: true
    ch-pdqm-constraints: true
```

Furthermore, the parameter `mag.ch-epr-fhir.epr-spid-as-patientid` allows to set the patient EPR-SPID as the FHIR 
Patient id. The mapping will automatically be changed to support that.
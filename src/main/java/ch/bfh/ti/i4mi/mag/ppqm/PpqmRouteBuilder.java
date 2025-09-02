package ch.bfh.ti.i4mi.mag.ppqm;

import ch.bfh.ti.i4mi.mag.config.props.MagPpqProps;
import org.apache.camel.builder.RouteBuilder;
import org.apache.cxf.binding.soap.SoapFault;
import org.openehealth.ipf.commons.ihe.fhir.chppqm.translation.FhirToXacmlTranslator;
import org.openehealth.ipf.commons.ihe.fhir.chppqm.translation.XacmlToFhirTranslator;
import org.openehealth.ipf.commons.ihe.xacml20.chppq.ChPpqMessageCreator;
import org.openehealth.ipf.commons.ihe.xacml20.stub.UnknownPolicySetIdFaultMessage;
import org.openehealth.ipf.platform.camel.core.util.Exchanges;

/**
 * @author Dmytro Rud
 */
abstract public class PpqmRouteBuilder extends RouteBuilder {

    protected final FhirToXacmlTranslator fhirToXacmlTranslator;
    protected final ChPpqMessageCreator ppqMessageCreator;
    protected final MagPpqProps ppqProps;

    protected PpqmRouteBuilder(
            final FhirToXacmlTranslator fhirToXacmlTranslator,
            final ChPpqMessageCreator ppqMessageCreator,
            final MagPpqProps ppqProps) {
        this.fhirToXacmlTranslator = fhirToXacmlTranslator;
        this.ppqMessageCreator = ppqMessageCreator;
        this.ppqProps = ppqProps;
    }

    protected void configureExceptionHandling() {

        onException(SoapFault.class)
                .handled(true)
                .maximumRedeliveries(0)
                .process(exchange -> {
                    log.debug("Received SOAP Fault, translate to FHIR");
                    Exception e = Exchanges.extractException(exchange);
                    SoapFault soapFault = (SoapFault) e.getCause();
                    XacmlToFhirTranslator.translateSoapFault(soapFault);
                })
        ;

        onException(UnknownPolicySetIdFaultMessage.class)
                .handled(true)
                .maximumRedeliveries(0)
                .process(exchange -> {
                    log.debug("Received UnknownPolicySetIdFault, translate to FHIR");
                    UnknownPolicySetIdFaultMessage fault = (UnknownPolicySetIdFaultMessage) Exchanges.extractException(
                            exchange);
                    XacmlToFhirTranslator.translateUnknownPolicySetIdFault(fault);
                })
        ;

        onException(Exception.class)
                .maximumRedeliveries(0)
        ;
    }

}

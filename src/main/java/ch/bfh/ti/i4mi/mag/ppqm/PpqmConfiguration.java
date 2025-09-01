package ch.bfh.ti.i4mi.mag.ppqm;

import ch.bfh.ti.i4mi.mag.config.props.MagProps;
import org.openehealth.ipf.commons.ihe.fhir.chppqm.translation.FhirToXacmlTranslator;
import org.openehealth.ipf.commons.ihe.xacml20.chppq.ChPpqMessageCreator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Dmytro Rud
 */
@Configuration
public class PpqmConfiguration {

    @Bean
    public ChPpqMessageCreator ppqMessageCreator(final MagProps magProps) {
        String homeCommunityId = magProps.getHomeCommunityId();
        if (!homeCommunityId.startsWith("urn:oid:")) {
            homeCommunityId = "urn:oid:" + homeCommunityId;
        }
        return new ChPpqMessageCreator(homeCommunityId);
    }

    @Bean
    public FhirToXacmlTranslator fhirToXacmlTranslator(ChPpqMessageCreator ppqMessageCreator) {
        return new FhirToXacmlTranslator(ppqMessageCreator);
    }
}

package ch.bfh.ti.i4mi.mag.audit;

import jakarta.annotation.PreDestroy;

import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.DefaultAuditContext;
import org.openehealth.ipf.commons.audit.codes.EventOutcomeIndicator;
import org.openehealth.ipf.commons.audit.event.ApplicationActivityBuilder;
import org.openehealth.ipf.commons.audit.protocol.TLSSyslogSenderImpl;
import org.openehealth.ipf.commons.audit.utils.AuditUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class Audit implements ApplicationListener<ApplicationReadyEvent> {

	@Autowired
	private AuditContext myAuditContext;
	
	@Value("${mag.name:Mobile Access Gateway}")
	private String appName;
	
	public void start() {
		
	}
	
	@PreDestroy
	public void onStop() {
		log.debug("Adding Application Stop Audit message");
		myAuditContext.audit(
			    new ApplicationActivityBuilder.ApplicationStop(EventOutcomeIndicator.Success)
			        .setAuditSource(myAuditContext)
			        .setApplicationParticipant(
			                appName,
			                null,
			                appName,
			                AuditUtils.getLocalHostName())
			        .addApplicationStarterParticipant(System.getProperty("user.name"))
			        .getMessage()
		);
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		log.debug("Adding Application Start Audit message");
		
		// Fix Problem: "TCP socket timeout reached, message may not have been completely received or socket has not been closed"
		// Force implementation that closes Socket after each message		
		if (myAuditContext instanceof DefaultAuditContext && myAuditContext.getAuditTransmissionProtocol() instanceof TLSSyslogSenderImpl) { 
			((DefaultAuditContext) myAuditContext).setAuditTransmissionProtocol(new TLSCloseSocket(myAuditContext.getTlsParameters()));
		}
		// End Fix Problem
		
		myAuditContext.audit(
			    new ApplicationActivityBuilder.ApplicationStart(EventOutcomeIndicator.Success)
			        .setAuditSource(myAuditContext)
			        .setApplicationParticipant(
			                appName,
			                null,
			                appName,
			                AuditUtils.getLocalHostName())
			        .addApplicationStarterParticipant(System.getProperty("user.name"))
			        .getMessage()
		);
	}
}

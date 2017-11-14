package my.edu.umk.pams.connector.route;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.sql.SqlComponent;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.jms.ConnectionFactory;
import javax.sql.DataSource;

import my.edu.umk.pams.connector.Application;
import my.edu.umk.pams.connector.model.CandidateMapper;
import my.edu.umk.pams.connector.payload.CandidatePayload;
import my.edu.umk.pams.connector.processor.CandidateQueueSyncProcessor;
import my.edu.umk.pams.connector.processor.StaffSyncProcessor;

@Component
public class ConnectorRoute extends RouteBuilder {

	private static final Logger LOG = LoggerFactory.getLogger(ConnectorRoute.class);

	@Autowired
	private ConnectionFactory connectionFactory;
	
	 @Autowired
	    private StaffSyncProcessor staffSyncProcessor;
	 
	 @Autowired
	    @Qualifier(value = "imsDataSource")
	    private DataSource imsDataSource;
	 
	@PostConstruct
	public void postConstruct() {
		LOG.info("Loading ConnectorRoute");
	}

	@Override
	public void configure() throws Exception {
		JmsComponent component = new JmsComponent();
		component.setConnectionFactory(connectionFactory);
		getContext().addComponent("jms", component);
		
		SqlComponent imsSqlComponent = new SqlComponent();
        imsSqlComponent.setDataSource(imsDataSource);
        getContext().addComponent("sqlIms", imsSqlComponent);
        
//        
//        from("quartz://syncTimer?cron={{sampleCronExpression}}")
//        .to("sqlIms:SELECT SM_STAFF_ID,SM_STAFF_NAME from STAFF_ALL WHERE SM_STAFF_NAME LIKE '%HANIF%'?useIterator=true")
//        .bean("staffMapper", "process")
//        .setProperty("staffId",body())
//        .setProperty("staffName",body())
//        .multicast().stopOnException()
//        .to("direct:academicTestImsStaff").end();
         
         from("jms:queue:academicTestStaffQueue2")
 		.log("incoming staff ims")
 		.setHeader(Exchange.HTTP_METHOD, constant("PUT"))
 		.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
 		.to("http4://{{rest.academic.host}}:{{rest.academic.port}}/api/integration/staff").end();

		from("jms:queue:imsStaffQueue")
		.routeId("imsStaffQueue")
		.log("IMS Staff Queue")
		.setHeader(Exchange.HTTP_METHOD, constant("POST"))
		.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
		.to("http4://{{rest.academic.host}}:{{rest.academic.port}}/api/integration/staff")
		.end();
		
		
		
		
		//Sending Multicast Candidate From Intake To Academic and Account
		from("jms:queue:candidateQueue3")
		.routeId("candidateQueueRoute3")
		.log("incoming candidate")
		.setHeader(Exchange.HTTP_METHOD, constant("POST"))
		.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
		.to("direct:academic","direct:account")
		.end();
		
		from("direct:academic")
		.log("Academic Candidate Route")
		.setHeader(Exchange.HTTP_METHOD, constant("POST"))
		.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
		.to("http4://{{rest.academic.host}}:{{rest.academic.port}}/api/integration/candidates").end();

		from("direct:account")
		.log("Account Candidate Route")
		.setHeader(Exchange.HTTP_METHOD, constant("POST"))
		.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
		.to("http4://{{rest.account.host}}:{{rest.account.port}}/api/integration/candidates")
		.end();
		
		//Admission Payload From Academic
		from("jms:queue:AdmissionPayloadQueue1")
		.routeId("AdmissionPayloadQueue1")
		.log("Incoming AdmissionPayloadQueue1")
		.setHeader(Exchange.HTTP_METHOD, constant("POST"))
		.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
		.to("http4://{{rest.account.host}}:{{rest.account.port}}/api/integration/admissions")
		.end();

		//Sending Student Account From Account
		from("jms:queue:accountQueue1")
		.routeId("accountQueue1")
		.log("incoming Account Student")
		.setHeader(Exchange.HTTP_METHOD, constant("PUT"))
		.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
		.to("http4://{{rest.academic.host}}:{{rest.academic.port}}/api/integration/studentAccounts")
		.end();
		
		//Testing Sending One To Many
		from("jms:queue:facultyCodeQueue2")
		.routeId("facultyCodeQueue2")
		.log("Incoming Faculty Code")
		.multicast()
		.to("direct:intake","direct:accountFaculty")
		.end();

		from("direct:intake")
		.log("intake faculty code")
		.setHeader(Exchange.HTTP_METHOD, constant("POST"))
		.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
		.to("http4://{{rest.intake.host}}:{{rest.intake.port}}/api/integration/facultyCodes").end();

		from("direct:accountFaculty")
		.log("account faculty code")
		.setHeader(Exchange.HTTP_METHOD, constant("POST"))
		.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
		.to("http4://{{rest.account.host}}:{{rest.account.port}}/api/integration/facultyCodes")
		.end();
		
		//Program
		from("jms:queue:programCodeQueue2")
		.routeId("programCodeQueue2")
		.log("incoming program code2")
		.multicast()
		.to("direct:intakeProgram","direct:accountProgram")
		.end();

		from("direct:intakeProgram")
		.log("intake faculty code")
		.setHeader(Exchange.HTTP_METHOD, constant("POST"))
		.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
		.to("http4://{{rest.intake.host}}:{{rest.intake.port}}/api/integration/programCodes").end();

		from("direct:accountProgram")
		.log("account faculty code")
		.setHeader(Exchange.HTTP_METHOD, constant("POST"))
		.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
		.to("http4://{{rest.account.host}}:{{rest.account.port}}/api/integration/programCodes")
		.end();
		
		//Guardian Payload From Academic
		from("jms:queue:GuardianPayloadQueue")
		.routeId("GuardianPayloadQueue")
		.log("incoming GuardianPayloadQueue")
		.setHeader(Exchange.HTTP_METHOD, constant("POST"))
		.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
		.to("http4://{{rest.account.host}}:{{rest.account.port}}/api/integration/guardians")
		.log("Finish GuardianPayloadQueue Routes")
		.end();
		
		from("jms:queue:MinAmountPayloadQueue")
		.routeId("MinAmountPayloadQueue")
		.log("incoming MinAmountPayloadQueue")
		.setHeader(Exchange.HTTP_METHOD, constant("POST"))
		.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
		.to("http4://{{rest.account.host}}:{{rest.account.port}}/api/integration/minAmounts")
		.log("Finish MinAmountPayloadQueue Routes")
		.end();
		
		



	}
}
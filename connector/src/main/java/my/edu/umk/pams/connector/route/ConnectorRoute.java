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

@Component
public class ConnectorRoute extends RouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectorRoute.class);

    @Autowired
    private ConnectionFactory connectionFactory;

    @PostConstruct
    public void postConstruct() {
        LOG.info("Loading ConnectorRoute");
    }

    @Override
    public void configure() throws Exception {
        JmsComponent component = new JmsComponent();
        component.setConnectionFactory(connectionFactory);
        getContext().addComponent("jms", component);

        from("jms:queue:candidateQueue")
                .routeId("candidateQueueRoute")
                .log("incoming candidate")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .to("http4://{{rest.academic.host}}:{{rest.academic.port}}/api/integration/candidates")
                .end();

        from("jms:queue:programCodeQueue")
                .routeId("programCodeQueue")
                .log("incoming program code")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .to("http4://{{rest.account.host}}:{{rest.account.port}}/api/integration/programCodes")
                .end();

        from("jms:queue:facultyCodeQueue")
                .routeId("facultyCodeQueue")
                .log("incoming faculty code")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .to("http4://{{rest.account.host}}:{{rest.account.port}}/api/integration/facultyCodes")
                .end();
        
        from("jms:queue:accountQueue")
		        .routeId("accountQueue")
		        .log("incoming Account Student")
		        .setHeader(Exchange.HTTP_METHOD, constant("PUT"))
		        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
		        .to("http4://{{rest.academic.host}}:{{rest.academic.port}}/api/integration/studentAccounts")
		        .end();
    }
}
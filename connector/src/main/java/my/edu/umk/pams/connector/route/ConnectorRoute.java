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
    @Qualifier(value = "intakeDataSource")
    private DataSource intakeDataSource;

    @Autowired
    @Qualifier(value = "intakeJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CandidateQueueSyncProcessor candidateQueueSyncProcessor;

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private CandidateMapper candidateMapper;

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
//                .bean(candidateMapper, "map")
//                .marshal().json(JsonLibrary.Jackson, CandidatePayload.class)
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .to("http4://{{rest.intake.host}}:{{rest.intake.port}}/api/integration/candidate")
                .end();
    }
}
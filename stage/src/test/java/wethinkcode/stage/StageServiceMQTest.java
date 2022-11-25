package wethinkcode.stage;


import java.util.concurrent.SynchronousQueue;

import javax.jms.*;

import kong.unirest.HttpResponse;
import kong.unirest.HttpStatus;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.*;
import wethinkcode.loadshed.common.transfer.StageDO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * I test StageService message sending.
 */
@Disabled
@Tag( "expensive" )
public class StageServiceMQTest
{
    public static final int TEST_PORT = 7777;

    private static StageService server;

    private static ActiveMQConnectionFactory factory;

    private static Connection mqConnection;

    @BeforeAll
    public static void startInfrastructure() throws JMSException {
        startMsgQueue();
        startStageSvc();
    }

    @AfterAll
    public static void cleanup() throws JMSException {
        server.stop();
        mqConnection.close();
    }

    @BeforeEach
    public void connectMqListener( MessageListener listener ) throws JMSException {
        mqConnection = factory.createConnection();
        final Session session = mqConnection.createSession( false, Session.AUTO_ACKNOWLEDGE );
        final Destination dest = session.createTopic( StageService.MQ_TOPIC_NAME );

        final MessageConsumer receiver = session.createConsumer( dest );
        receiver.setMessageListener( listener );

        mqConnection.start();
    }

    @AfterEach
    public void closeMqConnection() throws JMSException {
        mqConnection.close();
        mqConnection = null;
    }

    @Test
    public void sendMqEventWhenStageChanges(){
        System.out.println("vfbhbhjbhjfd");
        final SynchronousQueue<StageDO> resultCatcher = new SynchronousQueue<>();
        final MessageListener mqListener = message -> {
            long start = System.currentTimeMillis();
            if(message instanceof TextMessage){

                String body = null;
                try {
                    body = ((TextMessage) message).getText();
                } catch (JMSException e) {
                    e.printStackTrace();
                }

                if ("SHUTDOWN".equals(body)) {
                    long diff = System.currentTimeMillis() - start;
                    System.out.println(String.format("Received %d in %.2f seconds", (1.0 * diff / 1000.0)));
                    try {
                        mqConnection.close();
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(10);
                    } catch (Exception e) {}
                    System.exit(0);
                }
                System.out.println("Received message:" + body);

            }
        };

        final HttpResponse<StageDO> startStage = Unirest.get( serverUrl() + "/stage" ).asObject( StageDO.class );
        assertEquals( HttpStatus.OK, startStage.getStatus() );

        final StageDO data = startStage.getBody();
        final int newStage = data.getStage() + 1;

        final HttpResponse<JsonNode> changeStage = Unirest.post( serverUrl() + "/stage" )
            .header( "Content-Type", "application/json" )
            .body( new StageDO( newStage ))
            .asJson();
        assertEquals( HttpStatus.OK, changeStage.getStatus() );

        fail( "TODO" );


    }

    private static void startMsgQueue() throws JMSException {
        factory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
    }

    private static void startStageSvc(){
        server = new StageService().initialise();
        server.start( TEST_PORT );
    }

    private String serverUrl(){
        return "http://localhost:" + TEST_PORT;
    }
}

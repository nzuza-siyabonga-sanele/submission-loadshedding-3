package wethinkcode.stage;


import java.util.ArrayList;
import java.util.concurrent.SynchronousQueue;

import javax.jms.*;

import kong.unirest.HttpResponse;
import kong.unirest.HttpStatus;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.*;
import wethinkcode.loadshed.common.transfer.StageDO;

import static org.junit.jupiter.api.Assertions.*;

/**
 * I test StageService message sending.
 */
//@Disabled
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
    public static void cleanup(){
        server.stop();
//        mqConnection.close();
    }

//    @BeforeEach
    public void connectMqListener( MessageListener listener ) throws JMSException {

        mqConnection = factory.createConnection("admin", "admin");

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
    public void sendMqEventWhenStageChanges() throws JMSException {

        final SynchronousQueue<StageDO> resultCatcher = new SynchronousQueue<>();

        final MessageListener mqListener = message -> {

                if(message instanceof TextMessage){
                    String body = null;
                    try {
                        body = ((TextMessage) message).getText();
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                    int stageNumber = Integer.parseInt(body.split(":")[1].replaceAll("}",""));

                    Thread producer = new Thread("PRODUCER") {
                        public void run() {
                            try {
                                resultCatcher.put( new StageDO( stageNumber )); // thread will block here
                            } catch (InterruptedException e) { e.printStackTrace(); }
                        }
                    };producer.run();
                    producer.stop();
                }
        };

        connectMqListener(mqListener);

        final HttpResponse<StageDO> startStage = Unirest.get( serverUrl() + "/stage" ).asObject( StageDO.class );
        assertEquals( HttpStatus.OK, startStage.getStatus() );

        final StageDO data = startStage.getBody();
        final int newStage = data.getStage() + 1;

        HttpResponse<JsonNode> changeStage = Unirest.post( serverUrl() + "/stage" )
            .header( "Content-Type", "application/json" )
            .body( new StageDO( newStage ))
            .asJson();
        assertEquals( HttpStatus.OK, changeStage.getStatus() );


        final ArrayList<StageDO> eventLister = new ArrayList<>();
        Thread consumer = new Thread("CONSUMER") {
            public void run() {
                try {
                    StageDO event = resultCatcher.take();
                    eventLister.add( event );
            } catch (InterruptedException e) { e.printStackTrace(); } }
        }; consumer.run();
        consumer.stop();

        assertEquals( eventLister.get(0).getStage() , 1);
        assertTrue( eventLister.size() > 0);

    }

    private static void startMsgQueue(){
        factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
    }

    private static void startStageSvc(){
        server = new StageService().initialise();
        server.start( TEST_PORT );
    }

    private String serverUrl(){
        return "http://localhost:" + TEST_PORT;
    }
}

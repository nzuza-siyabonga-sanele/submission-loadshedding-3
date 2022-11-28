package wethinkcode.loadshed.spikes;

import javax.jms.*;

import org.apache.activemq.ActiveMQConnectionFactory;
import wethinkcode.loadshed.common.mq.MQ;



/**
 * I am a small "maker" app for receiving MQ messages from the Stage Service by
 * subscribing to a Topic.
 */
public class TopicReceiver implements Runnable {
    private static long NAP_TIME = 2000; //ms

    public static final String MQ_TOPIC_NAME = "stage";

    public static int stage;

    public static void main(String[] args) {
        final TopicReceiver app = new TopicReceiver();
        app.run();
    }

    private boolean running = true;

    private Connection connection;

    @Override
    public void run() {
        setUpMessageListener();
        while (running) {
            System.out.println("Still doing stufff...");
            snooze();
        }
        closeConnection();
        System.out.println("Bye...");
    }



    private void setUpMessageListener() {
        try {
            final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(MQ.URL);
            connection = factory.createConnection(MQ.USER, MQ.PASSWD);

            final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            final Destination dest = session.createTopic(MQ_TOPIC_NAME); // <-- NB: Topic, not Queue!
            final MessageConsumer receiver = session.createConsumer(dest);

            receiver.setMessageListener(msg -> {
                        long start = System.currentTimeMillis();

                        if(msg instanceof TextMessage){

                            String body = null;
                            try {
                                body = ((TextMessage) msg).getText();
                            } catch (JMSException e) {
                                e.printStackTrace();
                            }

                            if ("SHUTDOWN".equals(body)) {
                                long diff = System.currentTimeMillis() - start;
                                System.out.println(String.format("Received %d in %.2f seconds", (1.0 * diff / 1000.0)));
                                try {
                                    connection.close();
                                } catch (JMSException e) {
                                    e.printStackTrace();
                                }
                                try {
                                    Thread.sleep(10);
                                } catch (Exception e) {}
                                System.exit(0);
                            }
                            System.out.println("TopicReceiver Received message:" + body);
                            stage = createResponceStage( body );
                        }
//                throw new UnsupportedOperationException("TODO");
            }
            );
            connection.start();

        } catch (JMSException erk) {
            throw new RuntimeException(erk);
        }
    }

    public int getStage() {
        return stage;
    }

    private int createResponceStage(String body){
        String stageName = body.split(":")[0].replaceAll("\\{","");
        String stageNumber = body.split(":")[1].replaceAll("}","");
        if( checkInteger(stageNumber) ){
            return Integer.parseInt(stageNumber);
        }
        return stage;
    }

    private boolean checkInteger(String stageNumber) {
        try {
            Integer.parseInt( stageNumber );
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }

    private void snooze() {
        try {
            Thread.sleep(NAP_TIME);
        } catch (InterruptedException eek) {
            // meh...
        }
    }

    private void closeConnection() {
        if (connection != null) try {
            connection.close();
        } catch (JMSException ex) {
            // meh
        }
    }

}

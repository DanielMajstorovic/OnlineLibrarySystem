package org.unibl.etf.mdp.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.unibl.etf.mdp.config.ConfigLoader;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitMQHelper implements AutoCloseable{

    private Connection connection;
    private Channel channel;
    private String exchangeName;

    public RabbitMQHelper() throws IOException, TimeoutException {
        String host = ConfigLoader.getProperty("rabbitmq.host");
        int port = Integer.parseInt(ConfigLoader.getProperty("rabbitmq.port"));
        String username = ConfigLoader.getProperty("rabbitmq.username");
        String password = ConfigLoader.getProperty("rabbitmq.password");
        exchangeName = ConfigLoader.getProperty("rabbitmq.exchange");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);

        connection = factory.newConnection();
        channel = connection.createChannel();

        channel.exchangeDeclare(exchangeName, "direct", true);
    }

    public void sendMessage(String routingKey, String message) throws IOException {
        channel.basicPublish(exchangeName, routingKey, null, message.getBytes());
    }

    public void close() throws IOException, TimeoutException {
        if (channel != null) channel.close();
        if (connection != null) connection.close();
    }
}

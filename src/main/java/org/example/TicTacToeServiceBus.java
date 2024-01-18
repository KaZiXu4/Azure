package org.example;

import com.azure.core.util.BinaryData;
import com.azure.core.util.IterableStream;
import com.azure.messaging.servicebus.*;

import java.util.concurrent.TimeUnit;

public class TicTacToeServiceBus {
    private String connectionString;
    private TicTacToeGame game;

    public TicTacToeServiceBus(String connectionString) {
        this.connectionString = connectionString;
        this.game = new TicTacToeGame();
    }

    public void startGame() {
        while (game.checkWinner() == '-') {
            String move = receiveMessage();
            // Zakładając, że ruch jest w formacie "row,col"
            String[] parts = move.split(",");
            int row = Integer.parseInt(parts[0]);
            int col = Integer.parseInt(parts[1]);

            if (game.makeMove(row, col, game.getCurrentPlayer())) {
                sendMessage("Ruch wykonany: " + move);
                if (game.checkWinner() != '-') {
                    sendMessage("Gra zakończona. Wygrywa: " + game.getCurrentPlayer());
                    break;
                }
                game.switchPlayer();
            } else {
                sendMessage("Niepoprawny ruch: " + move);
            }
        }
    }

    private String receiveMessage() {
        ServiceBusReceiverClient receiverClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .receiver()
                .queueName("player1")
                .buildClient();

        // Odbierz wiadomość
        IterableStream<ServiceBusReceivedMessage> messages = receiverClient.receiveMessages(1);
        String messageBody = messages.stream().findFirst()
                .map(ServiceBusReceivedMessage::getBody)
                .map(BinaryData::toString)
                .orElse("0,0");

        // Zakończ połączenie
        receiverClient.close();

        return messageBody;
    }



    private void sendMessage(String messageContent) {
        ServiceBusSenderClient senderClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .queueName("player1")
                .buildClient();

        // Wyślij wiadomość
        senderClient.sendMessage(new ServiceBusMessage(messageContent));

        // Zakończ połączenie
        senderClient.close();
    }
}

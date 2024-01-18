package org.example;

import com.azure.messaging.servicebus.*;

import java.util.Scanner;

// ... (importy, klasy pomocnicze, etc.)

public class TicTacToeGame {

    private static final String connectionStringPlayer1 = "Endpoint=sb://lab04mm.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=y/rHwx2fcF4ZdZDVEcFzS/bq4GoOIdXXv+ASbNUsB3M=";
    private static final String connectionStringPlayer2 = "Endpoint=sb://lab04mm.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=y/rHwx2fcF4ZdZDVEcFzS/bq4GoOIdXXv+ASbNUsB3M=";
    private static final String queueNamePlayer1 = "player1";
    private static final String queueNamePlayer2 = "player2";
    private static boolean isPlayer1;
    private static ServiceBusSenderClient senderClient;
    private static ServiceBusProcessorClient processorClient;
    private static GameBoard board = new GameBoard(); // klasa GameBoard reprezentuje planszę

    public static void main(String[] args) {
        // Ustalenie, który gracz uruchamia klienta
        isPlayer1 = args.length > 0 && args[0].equals("player1");

        setupClients();
        startReceivingMessages();

        playGame();

        // Zamykanie klientów
        processorClient.close();
        senderClient.close();
    }

    private static void setupClients() {
        if (isPlayer1) {
            senderClient = createSenderClient(connectionStringPlayer2, queueNamePlayer2);
            processorClient = createProcessorClient(connectionStringPlayer1, queueNamePlayer1);
        } else {
            senderClient = createSenderClient(connectionStringPlayer1, queueNamePlayer1);
            processorClient = createProcessorClient(connectionStringPlayer2, queueNamePlayer2);
        }
    }

    private static ServiceBusSenderClient createSenderClient(String connectionString, String queueName) {
        return new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .queueName(queueName)
                .buildClient();
    }

    private static ServiceBusProcessorClient createProcessorClient(String connectionString, String queueName) {
        return new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .processor()
                .queueName(queueName)
                .processMessage(TicTacToeGame::processMessage)
                .processError(context -> System.out.println("Error occurred: " + context.getException()))
                .buildProcessorClient();
    }

    private static void startReceivingMessages() {
        processorClient.start();
    }

    private static void playGame() {
        Scanner scanner = new Scanner(System.in);
        while (!board.isGameOver()) {
            if ((board.isPlayer1Turn() && isPlayer1) || (!board.isPlayer1Turn() && !isPlayer1)) {
                System.out.println("Your turn. Enter row and column (e.g., '1 2'): ");
                int row = scanner.nextInt();
                int col = scanner.nextInt();
                board.makeMove(row, col);
                sendMove(row, col);
            } else {
                System.out.println("Waiting for opponent's move...");
                try {
                    Thread.sleep(2000); // czekanie na ruch przeciwnika
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        scanner.close();
    }

    private static void sendMove(int row, int col) {
        String messageContent = row + "," + col;
        ServiceBusMessage message = new ServiceBusMessage(messageContent);
        senderClient.sendMessage(message);
    }

    private static void processMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        String[] parts = message.getBody().toString().split(",");
        int row = Integer.parseInt(parts[0]);
        int col = Integer.parseInt(parts[1]);
        board.makeMove(row, col);
        System.out.println("Opponent made move: " + row + "," + col);
    }

    // Klasa GameBoard i inne metody pomocnicze...
}

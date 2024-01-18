package org.example;

import com.azure.messaging.servicebus.*;

import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TicTacToe {
    static String connectionString = "Endpoint=sb://lab04mm.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=y/rHwx2fcF4ZdZDVEcFzS/bq4GoOIdXXv+ASbNUsB3M=";
    static String queueName = "player1";
    static char[] board = new char[9];
    static boolean isPlayer1;

    public static void main(String[] args) throws InterruptedException {
        Arrays.fill(board, ' ');
        Scanner scanner = new Scanner(System.in);

        System.out.println("Czy jesteś Graczem 1? (tak/nie)");
        isPlayer1 = scanner.nextLine().trim().equalsIgnoreCase("tak");

        if (isPlayer1) {
            playGame(scanner);
        } else {
            listenForMoves();
        }
    }

    static void playGame(Scanner scanner) throws InterruptedException {
        while (true) {
            printBoard();
            System.out.println("Twój ruch (1-9): ");
            int move = scanner.nextInt() - 1;
            if (move < 0 || move >= 9 || board[move] != ' ') {
                System.out.println("Nieprawidłowy ruch, spróbuj ponownie.");
                continue;
            }

            board[move] = isPlayer1 ? 'X' : 'O';
            sendMessage(String.valueOf(move));

            if (isGameOver()) {
                printBoard();
                System.out.println("Gra zakończona!");
                break;
            }

            System.out.println("Oczekiwanie na ruch przeciwnika...");
            listenForMoves();
        }
    }

    static void listenForMoves() throws InterruptedException {
        CountDownLatch countdownLatch = new CountDownLatch(1);

        ServiceBusProcessorClient processorClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .processor()
                .queueName(queueName)
                .processMessage(context -> processMessage(context, countdownLatch))
                .processError(context -> processError(context, countdownLatch))
                .buildProcessorClient();

        processorClient.start();
        countdownLatch.await(2, TimeUnit.MINUTES);
        processorClient.close();
    }

    private static void processMessage(ServiceBusReceivedMessageContext context, CountDownLatch countdownLatch) {
        String messageBody = context.getMessage().getBody().toString();
        int move = Integer.parseInt(messageBody);
        board[move] = isPlayer1 ? 'O' : 'X';

        if (isGameOver()) {
            printBoard();
            System.out.println("Gra zakończona!");
        } else {
            countdownLatch.countDown();
        }
    }

    private static void processError(ServiceBusErrorContext context, CountDownLatch countdownLatch) {
        System.out.printf("Błąd: %s%n", context.getException());
        countdownLatch.countDown();
    }

    static void sendMessage(String message) {
        ServiceBusSenderClient senderClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .queueName(queueName)
                .buildClient();

        senderClient.sendMessage(new ServiceBusMessage(message));
        senderClient.close();
    }

    static void printBoard() {
        for (int i = 0; i < 9; i++) {
            if (i > 0 && i % 3 == 0) {
                System.out.println("\n---|---|---");
            } else if (i % 3 > 0) {
                System.out.print("|");
            }
            System.out.print(" " + board[i] + " ");
        }
        System.out.println();
    }

    static boolean isGameOver() {
        // Prosta logika sprawdzająca czy gra się zakończyła
        // ...
        return false; // Zwróć true, jeśli gra się zakończyła
    }
}

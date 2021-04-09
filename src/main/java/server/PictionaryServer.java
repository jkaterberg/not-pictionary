package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PictionaryServer {
    private static final int SERVER_PORT = 9000;
    private static final int MAX_PLAYERS = 4;

    protected Socket clientSocket = null;
    protected ServerSocket serverSocket = null;
    protected Player[] players = null;
    protected int numPlayers = 0;

    public PictionaryServer(){
        try{
            //Start the server
            serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("SERVER IS RUNNING...");
            System.out.println("LISTENING ON PORT " + SERVER_PORT);

            players = new Player[MAX_PLAYERS];

            //Listen for incoming connections until the max number of players is reached
            while(players.length != 4){
                clientSocket = serverSocket.accept();

                // Create a new Player object
                System.out.println("CONNECTED PLAYERS " + numPlayers);
                players[numPlayers] = new Player(clientSocket);
                players[numPlayers].pictionaryThread.start();
                numPlayers++;

                //Check for disconnected players and remove them from the list
                for(int i=0; i<players.length; i++){
                    if(players[i] != null && !players[i].pictionaryThread.isAlive()){
                        players[i].pictionaryThread.join();
                        for(int j=i; j<players.length-1; j++){
                            players[j] = players[j+1];
                        }
                        i=0;
                        numPlayers--;
                    }
                }
            }

            //Game time
            GameLogic game = new GameLogic(Arrays.asList(players));
            ArrayList<String> newMsgs = new ArrayList<>();
            Player drawer = null;
            boolean newRound = false;

            while(true){
                //pick the next player
                drawer = game.chooseDrawer();

                //game loop for each round
                while(!newRound) {
                    //get the drawing coordinates from the drawer
                    ArrayList<String> newCoords = new ArrayList<>();
                    drawer.coordinates.drainTo(newCoords);

                    //Get the new messages sent by the players
                    for(Player player : players){
                        String newMsg;
                        while((newMsg = player.guesses.take()) != null){
                            newMsgs.add(player.getUsername() + ": " + newMsg);
                            newRound = (game.isCorrectWord(newMsg));
                        }
                    }

                    //get the requests/data from each player, send them new drawing coordinates
                    for (Player player : players) {
                        //Send the new messages to all the players
                        player.sendMsg(newMsgs);

                        //don't check the player's guesses if they are drawing
                        if (!player.getDrawer()) {
                            //check the guesses
                            String guess;
                            while ((guess = player.guesses.take()) != null) {
                                newRound = game.isCorrectWord(guess);
                                break;
                            }
                            player.sendCoords(newCoords);
                        }
                    }
                    //Clear the new messages for the next time around
                    newMsgs.clear();
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        PictionaryServer server = new PictionaryServer();
    }
}
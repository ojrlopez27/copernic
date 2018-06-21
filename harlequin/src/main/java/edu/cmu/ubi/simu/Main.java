package edu.cmu.ubi.simu;

import edu.cmu.inmind.multiuser.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.common.CommonUtils;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.communication.ResponseListener;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.log.LogC;
import edu.cmu.ubi.simu.harlequin.control.HarlequinController;
import edu.cmu.ubi.simu.scenario.demo.SimuController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;


/**
 * Created by oscarr on 5/4/18.
 */

public class Main{
    public static final boolean useMUF = true;
    public static final boolean useSimu = true;

    public static void main(String args[]){

        // let's start the MUF (server) first
        if(useMUF) HarlequinController.getInstance().start();

//        CommonUtils.execute(new Runnable() {
//            @Override
//            public void run() {
//                CommonUtils.sleep(5000);
//                new Client("Alice", new String[]{"", "have a party", "I do the grocery shopping"});
//                CommonUtils.sleep(3000);
//                new Client("Bob", new String[]{"have a party", "I do the grocery shopping"});
//            }
//        });

        // now, let's start the simulation
        if(useSimu) SimuController.getInstance().start();
    }
}


class Client {
    private ClientCommController commController;
    private String serverAddress = "tcp://100.6.9.197:5555";
    private String sessionId;
    private ArrayList<String> commands;
    private Scanner scanner = new Scanner(System.in);

    public Client(String sessionId, String[] commands) {
        this.sessionId = sessionId;
        this.commands = new ArrayList<>(Arrays.asList(commands));
        this.commController = (new ClientCommController.Builder(new LogC()))
                .setServerAddress(this.serverAddress)
                .setSessionId(this.sessionId)
                .setRequestType("REQUEST_CONNECT")
                .setResponseListener(new MyResponseListener())
                .build();
    }

    public void send(String message) {
        try {
            SessionMessage sessionMessage = new SessionMessage();
            sessionMessage.setPayload(message);
            sessionMessage.setSessionId(this.sessionId);
            this.commController.send(this.sessionId, CommonUtils.toJson(sessionMessage));
        } catch (Exception var3) {
            var3.printStackTrace();
        }

    }

    class MyResponseListener implements ResponseListener, Runnable {
        MyResponseListener() {
        }

        public void process(String message) {
            if(message.contains(Constants.SESSION_INITIATED)){
                CommonUtils.execute(new Runnable() {
                    @Override
                    public void run() {
                        send("hi");
                    }
                });
            }else {
                System.out.println("Say something " + sessionId + "....");
                scanner.nextLine();
                String command = commands.remove(0);
                if (!command.isEmpty()) {
                    send(command);
                }
            }
        }

        @Override
        public void run() {
            while(!commands.isEmpty()){

            }
            while(!commController.getIsConnected().get()){
                CommonUtils.sleep(10);
            }
        }
    }
}




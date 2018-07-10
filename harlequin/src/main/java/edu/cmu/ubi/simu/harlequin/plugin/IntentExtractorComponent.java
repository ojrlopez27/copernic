package edu.cmu.ubi.simu.harlequin.plugin;

import edu.cmu.inmind.multiuser.controller.blackboard.Blackboard;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.plugin.StateType;
import edu.cmu.ubi.simu.harlequin.control.HarlequinController;
import edu.cmu.ubi.simu.harlequin.util.MsgConstants;
import edu.cmu.ubi.simu.scenario.demo.Constants.Events;

import java.util.regex.Pattern;

import static edu.cmu.ubi.simu.scenario.demo.Constants.Events.*;

/**
 * Created by oscarr on 5/7/18.
 */
@StateType(state = Constants.STATEFULL)
@BlackboardSubscription(messages = {MsgConstants.MSG_PROCESS_COMMAND})
public class IntentExtractorComponent extends PluggableComponent {

    private String sessionId;
    private HarlequinController harlequinController;

    public IntentExtractorComponent() {
        harlequinController = HarlequinController.getInstance();
    }

    @Override
    public void onEvent(Blackboard blackboard, BlackboardEvent blackboardEvent) throws Throwable {
        this.sessionId = getSessionId();
        Events simuStep = null;
        String command = ((String) blackboardEvent.getElement()).replace("\u0027", "\'");

        if( match(command, "*have*party*", "*having*party*") ){
            simuStep = S0_BOB_STARTS;
        }else if( match(command, "*I*do*grocery*shopping*") ){
            if(sessionId.equals("Bob")) simuStep = S9_BOB_DO_GROCERY;
            else if(sessionId.equals("Alice")) simuStep = S11_ALICE_DO_GROCERY;
        }else if( match(command, "*at*super*market*", "*at*supermarket*") ){
            if(sessionId.equals("Alice")) simuStep = S13_2_ALICE_AT_SUPERMARKET;
        }else if( match(command, "*Yes*carrying*my*drive*license*", "Yes") ){
            if(sessionId.equals("Bob")) simuStep = S13_BOB_GO_BEER_SHOP;
        }else if( match(command, "*done*what's next?*", "*next*") || command.contains("next?") ){
            if(sessionId.equals("Bob")) simuStep = S14_BOB_FIND_HOME_DECO;
        }else if( match(command, "*IKEA*") ){
            if(sessionId.equals("Bob")) simuStep = S15_BOB_GO_HOME_DECO;
        }else if( match(command, "*headache*pharmacy*ibuprofen*") || command.contains("ibuprofen") ){
            if(sessionId.equals("Alice")) simuStep = S16_ALICE_HEADACHE;
        }else if( match(command, "*CVS*") || command.contains("CVS") ){
            simuStep = S18_BOB_GO_PHARMACY;
        }else if( match(command, "*medication*") ){
            simuStep = S19_BOB_GO_HOME_DECO;
        }else if( match(command, "*here*") ){
            if(sessionId.equals("Alice")) simuStep = S21_GO_HOME;
        }

        if( simuStep != null ) {
            harlequinController.onUserCommandEvent(sessionId, command, simuStep);
        }
    }

    /**
     * A very simple template-based user intent extractor (acts as a simple NLU)
     * @param command
     * @param possibleMatches
     * @return
     */
    private boolean match(String command, String... possibleMatches){
        for(String possibleMatch : possibleMatches){
            if( Pattern.compile(possibleMatch.replace("*", "[a-zA-Z0-9_\\s\\?.,:'-]*"))
                    .matcher(command).matches() )
                return true;
        }
        return false;
    }

}

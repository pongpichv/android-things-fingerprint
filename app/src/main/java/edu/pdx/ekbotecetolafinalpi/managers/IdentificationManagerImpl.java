package edu.pdx.ekbotecetolafinalpi.managers;

import android.util.Log;

import edu.pdx.ekbotecetolafinalpi.states.IdentificationState;
import edu.pdx.ekbotecetolafinalpi.uart.Command;
import edu.pdx.ekbotecetolafinalpi.uart.CommandMap;
import edu.pdx.ekbotecetolafinalpi.uart.Response;

public class IdentificationManagerImpl extends FiniteStateMachineManager implements IdentificationManager {
    private static final String TAG = "IdentificationManagerIm";

    public IdentificationManagerImpl(UartManager uartManager, FirestoreManager dbManager) {
        super(uartManager, dbManager);
    }

    @Override
    protected void startStateMachine() {
        deviceDao.sendMessage("startStateMachine");
        state = IdentificationState.LED_ON;
        nextState = IdentificationState.FINGER_PRESS;
        uartManager.toggleLed(uartManager.LED_ON);
    }

    @Override
    void doAck(Response rsp) {
        switch (state) {
            case IdentificationState.NOT_ACTIVE:
                //do nothing
                break;
            default:
                step(rsp);
                break;
        }
    }

    @Override
    void doNack(Response rsp) {
        switch (state) {
            case IdentificationState.NOT_ACTIVE:
                //do nothing
                break;
            case IdentificationState.IDENT_START:
                deviceDao.sendMessage("Not authorized.");
                stopStateMachine();
                break;
            default:
                deviceDao.sendMessage("Error IDENT on state: " + state);
                deviceDao.sendMessage("Error: " + rsp.getError());
                stopStateMachine();
                break;
        }
    }
    
    private void step(Response rsp) {
        switch (state) {
            case IdentificationState.LED_ON:
                deviceDao.sendMessage("Place finger for identification...");
                state = nextState;
                nextState = IdentificationState.CAPTURE_FINGER;
                uartManager.queueCommand(new Command(0, CommandMap.IsPressFinger));
                break;
            case IdentificationState.FINGER_PRESS:
                if(rsp.getParams() > 0) {
                    getFingerPress();
                } else {
                    state = nextState;
                    nextState = IdentificationState.IDENT_START;
                    deviceDao.sendMessage("Capturing finger...(keep pressing)");
                    sendCommand(new Command(0, CommandMap.CaptureFinger));
                }
                break;
            case IdentificationState.CAPTURE_FINGER:
                state = nextState;
                nextState = IdentificationState.NOT_ACTIVE;
                deviceDao.sendMessage("Identify finger...(you can lift your finger)");
                sendCommand(new Command(0, CommandMap.Identify1_N));
                break;
            case IdentificationState.IDENT_START:
                deviceDao.sendMessage("Finger Identified as Scanner ID: " + rsp.getParams());
                stopStateMachine();
                break;
            default:
                Log.d(TAG, "step: " + state);
                break;
        }
    }

    @Override
    public void identifyFinger() {
        startStateMachine();
    }
}
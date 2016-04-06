package com.studio_terra.axolotldemo.model;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;


/**
 * ...
 */
public class Channel {
    private static final String TAG = "AxolotlDemo/Model";

    private List<Participant> m_participants;

    public Channel() {
        m_participants = new ArrayList<>();
    }

    public void connect(Participant participant) {
        m_participants.add(participant);
    }


    public void transmit(
            Participant fromParticipant,
            Participant toParticipant,
            byte[] encryptedMessage) {


        try {
            Log.v(TAG, "Channel.transmit(" + fromParticipant + ", " + toParticipant
                    + ", '" + URLEncoder.encode(new String(encryptedMessage), "utf-8")  + "')");
        } catch (UnsupportedEncodingException e) {
            Log.v(TAG, "Channel.transmit(" + fromParticipant + ", " + toParticipant
                    + ", encryptedMessage)");
        }

        try {
            toParticipant.receiveMessages(fromParticipant, encryptedMessage);

        } catch (Exception e) {
            Log.e(TAG, "Error in sending messages from "
                    + fromParticipant + " to " + toParticipant, e);
        }
    }

    public void broadcast(
            Participant fromParticipant,
            String fromGroup,
            List<Participant> toGroupParticipants,
            byte[] encryptedMessage) {

        try {
            Log.v(TAG, "Channel.broadcast(" + fromParticipant + ", '" + toGroupParticipants
                    + "', '" + URLEncoder.encode(new String(encryptedMessage), "utf-8")  + "')");
        } catch (UnsupportedEncodingException e) {
            Log.v(TAG, "Channel.transmit(" + fromParticipant + ", '" + toGroupParticipants
                    + "', encryptedMessage)");
        }

        for (Participant toParticipant : toGroupParticipants) {
            if (toParticipant == fromParticipant) continue;  // Skip owner

            Log.v(TAG, "... broadcast to " + toParticipant + " ...");

            try {
                toParticipant.receiveGroupMessages(fromGroup, fromParticipant, encryptedMessage);

            } catch (Exception e) {
                Log.e(TAG, "Error in sending messages from "
                        + fromParticipant + " to " + toParticipant, e);
            }
        }
    }
}

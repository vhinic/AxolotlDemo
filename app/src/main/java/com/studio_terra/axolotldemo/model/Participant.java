package com.studio_terra.axolotldemo.model;

import android.util.Base64;
import android.util.Log;

import org.whispersystems.libsignal.DuplicateMessageException;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.libsignal.InvalidVersionException;
import org.whispersystems.libsignal.LegacyMessageException;
import org.whispersystems.libsignal.NoSessionException;
import org.whispersystems.libsignal.SessionBuilder;
import org.whispersystems.libsignal.SessionCipher;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.UntrustedIdentityException;
import org.whispersystems.libsignal.ecc.ECPublicKey;
import org.whispersystems.libsignal.groups.GroupCipher;
import org.whispersystems.libsignal.groups.GroupSessionBuilder;
import org.whispersystems.libsignal.groups.SenderKeyName;
import org.whispersystems.libsignal.protocol.PreKeySignalMessage;
import org.whispersystems.libsignal.protocol.SenderKeyDistributionMessage;
import org.whispersystems.libsignal.protocol.SignalMessage;
import org.whispersystems.libsignal.state.PreKeyBundle;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignalProtocolStore;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.state.impl.InMemorySignalProtocolStore;
import org.whispersystems.libsignal.util.KeyHelper;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * ...
 */
public class Participant {
    private static final String TAG = "AxolotlDemo/Model";

    private static final int PREKEY_START = 1;
    private static final int PREKEY_COUNT = 100;
    private static final int SIGNED_PREKEY_ID = 1;

    private KeyServer m_keyServer;
    private Channel m_channel = null;
    private ParticipantInteractionListener m_listener;

    private SignalProtocolAddress m_address;
    private SignalProtocolStore m_store;
    private InMemorySenderKeyStore m_senderKeyStore;

    private boolean m_online = false;


    public Participant(KeyServer keyServer, Channel channel, String name, int deviceId)
            throws InvalidKeyException {
        Log.v(TAG, "Participant(keyServer, channel, " + name + ", " + deviceId + ")");

        m_keyServer = keyServer;
        m_channel = channel;
        m_online = true;
        m_address = new SignalProtocolAddress(name, deviceId);

        // For this demo always generate all necessary keys.
        // Normally, this would be done only first time newly installed app runs on the device,
        // and then it would be just loaded from appropriate SignalProtocolStore implementation
        // on consequent runs.
        int registrationId = KeyHelper.generateRegistrationId(false);
        IdentityKeyPair identityKeyPair = KeyHelper.generateIdentityKeyPair();
        PreKeyRecord lastResortKey = KeyHelper.generateLastResortPreKey();
        List<PreKeyRecord> preKeys = KeyHelper.generatePreKeys(PREKEY_START, PREKEY_COUNT);
        SignedPreKeyRecord signedPreKey =
                KeyHelper.generateSignedPreKey(identityKeyPair, SIGNED_PREKEY_ID);

        // Store all keys to appropriate SignalProtocolStore implementation.
        Map<Integer, ECPublicKey> publicPreKeys = new HashMap<Integer, ECPublicKey>();
        m_store = new InMemorySignalProtocolStore(identityKeyPair, registrationId);
        m_store.storeSignedPreKey(signedPreKey.getId(), signedPreKey);
        m_store.storePreKey(lastResortKey.getId(), lastResortKey);
        for (PreKeyRecord preKey : preKeys) {
            m_store.storePreKey(preKey.getId(), preKey);
            publicPreKeys.put(preKey.getId(), preKey.getKeyPair().getPublicKey());
        }

        // Group Store
        m_senderKeyStore = new InMemorySenderKeyStore();


        // Register with server by sending required info including PreKeys
        m_keyServer.register(
                m_address,
                identityKeyPair.getPublicKey(),
                registrationId,
                publicPreKeys,
                signedPreKey.getId(),
                signedPreKey.getKeyPair().getPublicKey(),
                signedPreKey.getSignature());
    }


    public void sendMessage(Participant toParticipant, String message)
            throws UntrustedIdentityException, InvalidKeyException {
        Log.v(TAG, "Participant.sendMessage(" + toParticipant + ", '" + message + "')");

        SignalProtocolAddress toAddress = toParticipant.getAddress();

        if (!m_store.containsSession(toAddress)) {
            SessionBuilder sessionBuilder = new SessionBuilder(m_store, toAddress);
            PreKeyBundle retrievedPreKey = m_keyServer.getPreKeyBundle(toAddress);
            sessionBuilder.process(retrievedPreKey);
        }

        SessionCipher sessionCipher = new SessionCipher(m_store, toAddress);
        byte[] encryptedMessage =
                sessionCipher.encrypt(message.getBytes(StandardCharsets.UTF_8)).serialize();

        m_channel.transmit(this, toParticipant, encryptedMessage);
    }

    public void joinGroup(String groupName) {
        Log.v(TAG, "Participant.joinGroup('" + groupName + "')");

        SenderKeyName senderKeyName = new SenderKeyName(groupName, this.getAddress());
        GroupSessionBuilder groupSessionBuilder = new GroupSessionBuilder(m_senderKeyStore);
        SenderKeyDistributionMessage senderDistributionMessage
                = groupSessionBuilder.create(senderKeyName);

        List<Participant> groupMembers = m_keyServer.getGrpoupMemebers(groupName);
        m_channel.broadcast(this, groupName, groupMembers, senderDistributionMessage.serialize());
    }

    public void sendGroupMessage(String groupName, String message) throws NoSessionException {
        Log.v(TAG, "Participant.sendGroupMessage('" + groupName + ", '" + message + "')");

        SenderKeyName senderKeyName = new SenderKeyName(groupName, this.getAddress());
        GroupCipher groupCipher = new GroupCipher(m_senderKeyStore, senderKeyName);

        byte[] encryptedMessage =
                groupCipher.encrypt(message.getBytes(StandardCharsets.UTF_8));

        List<Participant> groupMembers = m_keyServer.getGrpoupMemebers(groupName);
        m_channel.broadcast(this, groupName, groupMembers, encryptedMessage);
    }

    public void receiveMessages(Participant fromParticipant, byte[] encryptedMessage)
            throws InvalidMessageException, InvalidVersionException,
            InvalidKeyException, DuplicateMessageException, InvalidKeyIdException,
            UntrustedIdentityException, LegacyMessageException {

        Log.v(TAG, "Participant.receiveMessages(" + fromParticipant + ", encryptedMessages)");
        SessionCipher sessionCipher = new SessionCipher(m_store, fromParticipant.getAddress());

        byte[] messageBytes = null;
        try {
            // Try first to interpret message as WHISPER_TYPE
            // (there has to be better way to check message type)
            SignalMessage signalMessage = new SignalMessage(encryptedMessage);
            messageBytes = sessionCipher.decrypt(signalMessage);
        } catch (InvalidMessageException ime) {
        } catch (NoSessionException e) {
        }

        if (messageBytes == null) {
            Log.v(TAG, "... received PREKEY_TYPE CiphertextMessage ...");
            // Since message was not WHISPER_TYPE try to interpret it as PREKEY_TYPE
            PreKeySignalMessage preKeySignalMessage =
                    new PreKeySignalMessage(encryptedMessage);
            messageBytes = sessionCipher.decrypt(preKeySignalMessage);
        } else {
            Log.v(TAG, "... received WHISPER_TYPE CiphertextMessage ...");
        }

        String message = new String(messageBytes, StandardCharsets.UTF_8);

        Log.v(TAG, "... message: '" + message + "'");
        byte[] encodedEncryptedMessage = Base64.encode(encryptedMessage, Base64.DEFAULT);
        m_listener.onReceived(fromParticipant,
                new String(encodedEncryptedMessage, StandardCharsets.UTF_8), message);
    }

    public void receiveGroupMessages(
            String fromGroup, Participant fromParticipant, byte[]  encryptedMessage)
            throws InvalidMessageException, InvalidVersionException,
            InvalidKeyException, DuplicateMessageException, InvalidKeyIdException,
            UntrustedIdentityException, LegacyMessageException {

        Log.v(TAG, "Participant.receiveGroupMessages('"
                + fromGroup + "', " + fromParticipant + ", encryptedMessages)");

        SenderKeyName senderKeyName = new SenderKeyName(fromGroup, fromParticipant.getAddress());

        byte[] messageBytes = null;
        try {
            SenderKeyDistributionMessage senderKeyDistributionMessage =
                    new SenderKeyDistributionMessage(encryptedMessage);
            GroupSessionBuilder groupSessionBuilder =
                    new GroupSessionBuilder(m_senderKeyStore);
            groupSessionBuilder.process(senderKeyName, senderKeyDistributionMessage);
            Log.v(TAG, "... received SENDERKEY_DISTRIBUTION_TYPE CiphertextMessage ...");
        } catch (InvalidMessageException ime) {
            try {
                Log.v(TAG, "... received Group CiphertextMessage ...");
                GroupCipher groupCipher = new GroupCipher(m_senderKeyStore, senderKeyName);
                messageBytes = groupCipher.decrypt(encryptedMessage);
                String message = new String(messageBytes, StandardCharsets.UTF_8);

                Log.v(TAG, "... message: '" + message + "'");
                byte[] encodedEncryptedMessage = Base64.encode(encryptedMessage, Base64.DEFAULT);
                m_listener.onGroupReceived(fromParticipant,
                        new String(encodedEncryptedMessage, StandardCharsets.UTF_8), message);
            } catch (NoSessionException e) {
                Log.d(TAG, m_address + " has no group session for " + fromGroup);
            } catch (DuplicateMessageException dme) {
                // Ignore duplicate messages - workaround for this demo...
            }
        }
    }

    public SignalProtocolAddress getAddress() {
        return m_address;
    }


    public boolean isOnline() {
        return m_online;
    }


    public void setOnline(boolean online) {
        m_online = online;
    }


    @Override
    public String toString() {
        return m_address.toString();
    }


    public void setParticipantInteractionListener(ParticipantInteractionListener listener) {
        m_listener = listener;
    }

    /**
     * This interface must be implemented by activities that use
     * this participant to handle received messages.
     */
    public interface ParticipantInteractionListener {
        void onReceived(Participant fromParticipant, String encryptedMessage, String message);
        void onGroupReceived(Participant fromParticipant, String encryptedMessage, String message);
    }
}

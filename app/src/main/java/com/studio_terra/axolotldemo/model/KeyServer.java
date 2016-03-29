package com.studio_terra.axolotldemo.model;

import android.util.Log;

import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.ecc.ECPublicKey;
import org.whispersystems.libsignal.state.PreKeyBundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * ...
 */
public class KeyServer {
    private static final String TAG = "AxolotlDemo/Model";

    private Map<SignalProtocolAddress, KeyBundleRecord> m_keyStore;
    private Map<String, List<Participant>> m_groups;

    private class KeyBundleRecord {
        public IdentityKey identityKey;
        public int registrationId;
        public Map<Integer, ECPublicKey> preKeys;
        public int signedPreKeyId;
        public ECPublicKey signedPreKey;
        public byte[] signedPreKeySignature;
    }

    /**
     * ...
     */
    public KeyServer() {
        m_keyStore = new HashMap<SignalProtocolAddress, KeyBundleRecord>();
        m_groups = new HashMap<String, List<Participant>>();
    }

    /**
     * Register participant with specific address and set of required keys.
     *
     * @param address
     * @param identityKey
     * @param registrationId
     * @param preKeys
     * @param signedPreKeyId
     * @param signedPreKey
     * @param signedPreKeySignature
     */
    public void register(
            SignalProtocolAddress address,
            IdentityKey identityKey,
            int registrationId,
            Map<Integer, ECPublicKey> preKeys,
            int signedPreKeyId,
            ECPublicKey signedPreKey,
            byte[] signedPreKeySignature) {

        Log.v(TAG, "KeyServer.register(" + address + ", ...)");

        KeyBundleRecord keyBundleRecord = new KeyBundleRecord();
        keyBundleRecord.identityKey = identityKey;
        keyBundleRecord.registrationId = registrationId;
        keyBundleRecord.preKeys = preKeys;
        keyBundleRecord.signedPreKeyId = signedPreKeyId;
        keyBundleRecord.signedPreKey = signedPreKey;
        keyBundleRecord.signedPreKeySignature = signedPreKeySignature;

        m_keyStore.put(address, keyBundleRecord);
        Log.v(TAG, "... key bundle record stored ...");
        Log.v(TAG, "... total number of records stored is now " + m_keyStore.size() + " ...");
    }

    /**
     * Returns PreKeyBundle for give participant address.
     *
     * This method implement simple algorithm that just takes next consecutive PreKey
     * from registration set and builds PreKeyBundle using that PreKey.
     *
     * @param address of participant
     * @return next PreKeyBundle
     */
    public PreKeyBundle getPreKeyBundle(SignalProtocolAddress address) {
        Log.v(TAG, "KeyServer.getPreKeyBundle(" + address + ")");

        PreKeyBundle preKeyBundle = null;

        KeyBundleRecord keyBundleRecord = m_keyStore.get(address);

        if (keyBundleRecord != null) {

            if (!keyBundleRecord.preKeys.isEmpty()) {
                int preKeyId = keyBundleRecord.preKeys.keySet().iterator().next();
                preKeyBundle = new PreKeyBundle(
                        keyBundleRecord.registrationId,
                        address.getDeviceId(),
                        preKeyId,
                        keyBundleRecord.preKeys.get(preKeyId),
                        keyBundleRecord.signedPreKeyId,
                        keyBundleRecord.signedPreKey,
                        keyBundleRecord.signedPreKeySignature,
                        keyBundleRecord.identityKey);

                keyBundleRecord.preKeys.remove(preKeyId);  // removed used public PreKey

            } else {
                Log.i(TAG, "No more pre keys for " + address);
            }
        } else {
            Log.i(TAG, "Key bundle record not found for " + address);
        }

        return preKeyBundle;
    }

    /**
     * ...
     */
    public void registerWithGroup(String groupName, Participant participant) {
        Log.v(TAG, "KeyServer.registerWithGroup(" + groupName + ", " + participant + ")");
        List<Participant> groupMembers = m_groups.get(groupName);

        if (groupMembers == null) {
            groupMembers = new ArrayList<>();
        }
        groupMembers.add(participant);
        m_groups.put(groupName, groupMembers);
    }

    /**
     * ...
     */
    public List<Participant> getGrpoupMemebers(String groupName) {
        Log.v(TAG, "KeyServer.getGrpoupMemebers(" + groupName + ")");
        return m_groups.get(groupName);
    }
}

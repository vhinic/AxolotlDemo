package com.studio_terra.axolotldemo;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.studio_terra.axolotldemo.model.Channel;
import com.studio_terra.axolotldemo.model.KeyServer;
import com.studio_terra.axolotldemo.model.Participant;


/**
 * ...
 */
public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        ChatFragment.OnChatFragmentInteractionListener,
        GroupFragment.OnGroupFragmentInteractionListener,
        Participant.ParticipantInteractionListener {

    private static final String TAG = "AxolotlDemo";
    private static final String GROUP_NAME = "Group Chat";

    private KeyServer m_keyServer = null;
    private Channel m_channel = null;
    private Participant m_participantAlice = null;
    private Participant m_participantBob = null;
    private Participant m_participantCharlie = null;

    private ChatFragment mChatFragment = new ChatFragment();
    private GroupFragment mGroupFragment = new GroupFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "MainActivity.onCreate(...)");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_chat);
        navigationView.getMenu().performIdentifierAction(R.id.nav_chat, 0);

        try {
            Log.v(TAG, "... connect to the key server ...");
            m_keyServer = new KeyServer();

            Log.v(TAG, "... open to the communication channel ...");
            m_channel = new Channel();

            Log.v(TAG, "... create and register participants ...");
            m_participantAlice = new Participant(m_keyServer, m_channel, "Alice", 1);
            m_participantAlice.setParticipantInteractionListener(this);
            m_participantBob = new Participant(m_keyServer, m_channel, "Bob", 1);
            m_participantBob.setParticipantInteractionListener(this);
            m_participantCharlie = new Participant(m_keyServer, m_channel, "Charlie", 1);

            m_participantCharlie.setParticipantInteractionListener(this);

        } catch (Exception e) {
            Log.e(TAG, "Unexpected error", e);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Fragment fragment = null;

        int id = item.getItemId();

        if (id == R.id.nav_group_chat) {
            fragment = mGroupFragment;
            try {
                Log.v(TAG, "... Initiate group '" + GROUP_NAME + "' ...");
                m_keyServer.registerWithGroup(GROUP_NAME, m_participantAlice);
                m_keyServer.registerWithGroup(GROUP_NAME, m_participantBob);
                m_keyServer.registerWithGroup(GROUP_NAME, m_participantCharlie);
                m_participantAlice.joinGroup(GROUP_NAME);
                m_participantBob.joinGroup(GROUP_NAME);
                m_participantCharlie.joinGroup(GROUP_NAME);

            } catch (Exception e) {
                Log.e(TAG, "Unexpected error", e);
            }
        } else {
            fragment = mChatFragment;
        }

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.flContent, fragment).commit();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onChatSend(int editTextId, String message) {
        Log.v(TAG, "MainActivity.onChatSend(...)");

        try {
            if (editTextId == R.id.aliceEditText) {
                m_participantAlice.sendMessage(m_participantBob, message);
            } else {
                m_participantBob.sendMessage(m_participantAlice, message);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in sending message", e);
        }
    }

    @Override
    public void onGroupSend(int editTextId, String message) {
        Log.v(TAG, "MainActivity.onGroupSend(...)");

        try {
            if (editTextId == R.id.aliceEditText) {
                m_participantAlice.sendGroupMessage(GROUP_NAME, message);
            } else if (editTextId == R.id.bobEditText) {
                m_participantBob.sendGroupMessage(GROUP_NAME, message);
            } else {
                m_participantCharlie.sendGroupMessage(GROUP_NAME, message);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in sending message", e);
        }
    }

    @Override
    public void onReceived(Participant fromParticipant, String encryptedMessage, String message) {
        Log.v(TAG, "MainActivity.onReceived(" + fromParticipant + ", ...)");
        if (fromParticipant == m_participantBob) {
            mChatFragment.appendChatText(R.id.aliceTextView, fromParticipant + "> " + encryptedMessage);
            mChatFragment.appendChatText(R.id.aliceTextView, fromParticipant + "> " + message);
        } else if (fromParticipant == m_participantAlice) {
            mChatFragment.appendChatText(R.id.bobTextView, fromParticipant + "> " + encryptedMessage);
            mChatFragment.appendChatText(R.id.bobTextView, fromParticipant + "> " + message);
        }
    }

    public void onGroupReceived(Participant fromParticipant, String encryptedMessage, String message) {
        Log.v(TAG, "MainActivity.onReceived(" + fromParticipant + ", ...)");
        if (fromParticipant == m_participantBob) {
            mGroupFragment.appendChatText(R.id.aliceTextView, fromParticipant + "> " + encryptedMessage);
            mGroupFragment.appendChatText(R.id.aliceTextView, fromParticipant + "> " + message);
            mGroupFragment.appendChatText(R.id.charlieTextView, fromParticipant + "> " + encryptedMessage);
            mGroupFragment.appendChatText(R.id.charlieTextView, fromParticipant + "> " + message);
        } else if (fromParticipant == m_participantAlice) {
            mGroupFragment.appendChatText(R.id.bobTextView, fromParticipant + "> " + encryptedMessage);
            mGroupFragment.appendChatText(R.id.bobTextView, fromParticipant + "> " + message);
            mGroupFragment.appendChatText(R.id.charlieTextView, fromParticipant + "> " + encryptedMessage);
            mGroupFragment.appendChatText(R.id.charlieTextView, fromParticipant + "> " + message);
        } else if (fromParticipant == m_participantCharlie) {
            mGroupFragment.appendChatText(R.id.bobTextView, fromParticipant + "> " + encryptedMessage);
            mGroupFragment.appendChatText(R.id.bobTextView, fromParticipant + "> " + message);
            mGroupFragment.appendChatText(R.id.aliceTextView, fromParticipant + "> " + encryptedMessage);
            mGroupFragment.appendChatText(R.id.aliceTextView, fromParticipant + "> " + message);
        }
    }
}

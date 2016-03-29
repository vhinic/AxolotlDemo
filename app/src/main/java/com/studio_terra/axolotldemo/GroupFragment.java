package com.studio_terra.axolotldemo;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;


/**
 * ...
 */
public class GroupFragment extends Fragment {
    private static final String TAG = "AxolotlDemo";

    private OnGroupFragmentInteractionListener mListener;
    private ScrollView m_aliceScrollView;
    private TextView m_aliceTextView;
    private ScrollView m_bobScrollView;
    private TextView m_bobTextView;
    private ScrollView m_charlieScrollView;
    private TextView m_charlieTextView;

    public GroupFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_group, container, false);

        m_aliceScrollView  = (ScrollView) view.findViewById(R.id.aliceScrollView);
        m_aliceTextView  = (TextView) view.findViewById(R.id.aliceTextView);
        m_bobScrollView  = (ScrollView) view.findViewById(R.id.bobScrollView);
        m_bobTextView  = (TextView) view.findViewById(R.id.bobTextView);
        m_charlieScrollView  = (ScrollView) view.findViewById(R.id.charlieScrollView);
        m_charlieTextView  = (TextView) view.findViewById(R.id.charlieTextView);

        ImageButton aliceSendButton = (ImageButton) view.findViewById(R.id.aliceSendButton);
        aliceSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, "ChatFragment.aliceSendButton.onClick(...)");
                EditText aliceEditText = (EditText) view.findViewById(R.id.aliceEditText);
                Log.v(TAG, "... message: " + aliceEditText.getText().toString());
                send(R.id.aliceEditText, aliceEditText.getText().toString());
                aliceEditText.setText("");
            }
        });

        ImageButton bobSendButton = (ImageButton) view.findViewById(R.id.bobSendButton);
        bobSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, "ChatFragment.bobSendButton.onClick(...)");
                EditText bobEditText = (EditText) view.findViewById(R.id.bobEditText);
                Log.v(TAG, "... message: " + bobEditText.getText().toString());
                send(R.id.bobEditText, bobEditText.getText().toString());
                bobEditText.setText("");
            }
        });

        ImageButton charlieSendButton = (ImageButton) view.findViewById(R.id.charlieSendButton);
        charlieSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, "ChatFragment.charlieSendButton.onClick(...)");
                EditText charlieEditText = (EditText) view.findViewById(R.id.charlieEditText);
                Log.v(TAG, "... message: " + charlieEditText.getText().toString());
                send(R.id.charlieEditText, charlieEditText.getText().toString());
                charlieEditText.setText("");
            }
        });

        return view;
    }

    public void send(int name, String message) {
        if (mListener != null) {
            mListener.onGroupSend(name, message);
        }
    }

    public void appendChatText(int textViewId, String message) {
        if (textViewId == R.id.aliceTextView) {
            m_aliceTextView.append("\n" + message);
            m_aliceScrollView.post(new Runnable() {
                public void run() {
                    m_aliceScrollView.fullScroll(View.FOCUS_DOWN);
                }
            });
        } else if (textViewId == R.id.bobTextView) {
            m_bobTextView.append("\n" + message);
            m_bobScrollView.post(new Runnable() {
                public void run() {
                    m_bobScrollView.fullScroll(View.FOCUS_DOWN);
                }
            });
        } else {
            m_charlieTextView.append("\n" + message);
            m_charlieScrollView.post(new Runnable() {
                public void run() {
                    m_charlieScrollView.fullScroll(View.FOCUS_DOWN);
                }
            });
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnGroupFragmentInteractionListener) {
            mListener = (OnGroupFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnGroupFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnGroupFragmentInteractionListener {
        void onGroupSend(int editTextId, String message);
    }
}

package com.example.app;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.gms.maps.SupportMapFragment;

public class TouchableMapFragment extends SupportMapFragment {

    private OnTouchListener mListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, parent, savedInstanceState);

        TouchableWrapper touchWrapper = new TouchableWrapper(getActivity());
        touchWrapper.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        ((ViewGroup) view).addView(touchWrapper);
        return view;
    }

    public void setTouchListener(OnTouchListener listener) {
        mListener = listener;
    }

    public interface OnTouchListener {
        void onTouch();
        void onRelease();
    }

    private class TouchableWrapper extends FrameLayout {

        public TouchableWrapper(Context context) {
            super(context);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (mListener != null) mListener.onTouch();
                    break;
                case MotionEvent.ACTION_UP:
                    if (mListener != null) mListener.onRelease();
                    break;
            }
            return super.dispatchTouchEvent(event);
        }
    }
}
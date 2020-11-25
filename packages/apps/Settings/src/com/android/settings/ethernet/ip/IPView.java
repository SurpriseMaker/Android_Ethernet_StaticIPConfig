package com.android.settings.ethernet.ip;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class IPView extends AbsEditTextGroup {
    public IPView(Context context) {
        super(context);
    }

    public IPView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public IPView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (int i = 0; i < getChildCount(); i++) {
            if (i % 2 == 0) {
                AbsEditText absEditText= (AbsEditText) getChildAt(i);
                absEditText.setEnabled(enabled);
            }
        }
    }

    @Override
    public int getChildCount() {
        return 7;
    }

    @Override
    public AbsEditText getAbsEditText() {
        return new IPEditText(getContext());
    }

    @Override
    public String getSemicolomText() {
        return ".";
    }

    @Override
    public int getDelMaxLength() {
        return 3;
    }

    @Override
    public void applySemicolonTextViewTheme(TextView semicolonTextView) {
        semicolonTextView.setPadding(0,0,0,5);
        semicolonTextView.getPaint().setFakeBoldText(true);
        semicolonTextView.setBackgroundColor(0xFFFFFFFF);
        semicolonTextView.setGravity(Gravity.BOTTOM);
    }

    @Override
    public void applyEditTextTheme(AbsEditText absEditText) {

    }

}

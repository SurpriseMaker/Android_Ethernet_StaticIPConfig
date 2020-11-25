package com.android.settings.ethernet.ip;

import android.content.Context;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.NumberKeyListener;
import android.util.AttributeSet;
import android.widget.EditText;

public abstract class AbsEditText extends EditText {
    public AbsEditText(Context context) {
        this(context,null,0);
    }

    public AbsEditText(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public AbsEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setMaxLength();
        addInputFilter();
    }

    protected void setMaxLength(){
        setFilters(new InputFilter[]{new InputFilter.LengthFilter(getMaxLength())});
    }

    protected void addInputFilter(){
        setKeyListener(new NumberKeyListener() {
            @Override
            protected char[] getAcceptedChars() {
                return getInputFilterAcceptedChars();
            }

            @Override
            public int getInputType() {
                return InputType.TYPE_CLASS_NUMBER;
            }
        });
    }

    public abstract int getMaxLength();

    public abstract char[] getInputFilterAcceptedChars();

    public abstract boolean checkInputValue();


}

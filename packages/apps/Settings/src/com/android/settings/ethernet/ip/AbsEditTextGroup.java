package com.android.settings.ethernet.ip;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.R;

import java.util.ArrayList;

public abstract class AbsEditTextGroup extends LinearLayout implements TextWatcher {

    protected float sp16 = 16.0f;
    protected int dp4 = 4;
    private ArrayList<AbsEditText> editTexts = new ArrayList<AbsEditText>();

    public AbsEditTextGroup(Context context) {
        this(context, null, 0);
    }

    public AbsEditTextGroup(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AbsEditTextGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        addViews();
        buildListener();
    }

    protected void addViews() {
        for (int i = 0; i < getChildCount(); i++) {
            if (i%2==0) {
                AbsEditText absEditText= createAbsEditText();
                editTexts.add(absEditText);
                addView(absEditText);
            } else {
                addView(createSemicolonTextView());
            }
        }
    }

    protected AbsEditText createAbsEditText() {

        AbsEditText absEditText = getAbsEditText();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);
        params.weight = 1;
        absEditText.setLayoutParams(params);
        absEditText.setTextSize(sp16);
        absEditText.setTextColor(0xFF222222);
        absEditText.setGravity(Gravity.CENTER);
        absEditText.setPadding(dp4, dp4, dp4, dp4);
        absEditText.setSingleLine();
        absEditText.setFocusableInTouchMode(true);
        absEditText.setBackgroundColor(0xFFFFFFFF);
        applyEditTextTheme(absEditText);
        return absEditText;
    }

    protected TextView createSemicolonTextView() {
        TextView textView = new TextView(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        textView.setLayoutParams(params);
        textView.setTextSize(sp16);
        textView.setTextColor(0xFF444444);
        textView.setText(getSemicolomText());
        applySemicolonTextViewTheme(textView);
        return textView;
    }

    protected void buildListener() {
        for (int i = 0; i < editTexts.size(); i++) {
            editTexts.get(i).addTextChangedListener(this);
            if(i!=0){
                editTexts.get(i).setOnKeyListener(new OnDelKeyListener(editTexts.get(i-1), editTexts.get(i)));
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        android.util.Log.i("zhouyj", "start = " + start + " before = " + before + " count = " + count);
    }

    @Override
    public void afterTextChanged(Editable s) {
        String text = s.toString().trim();
        if (text.length() > 0 && Integer.parseInt(text.toString().trim()) > 255) {
            s.delete(text.length() - 2, 1);
            Toast.makeText(getContext(), R.string.eth_settings_error, Toast.LENGTH_SHORT).show();
        } else if (text.length() == getDelMaxLength()) {
            for (int i=0; i< editTexts.size()-1; i++){
                if(editTexts.get(i).hasFocus()){
                    editTexts.get(i).clearFocus();
                    editTexts.get(i+1).requestFocus();
                    break;
                }
            }
        }
    }

    public boolean checkInputValue(AbsEditText... params) {
        boolean result = true;
        for (int i = 0; i < params.length - 1; i++) {
            if (!params[i].checkInputValue()) {
                result = false;
                break;
            }
        }

        return result;
    }

    public void setIp(String ip) {
        if (!TextUtils.isEmpty(ip)) {
            String[] ips = ip.split("\\" + getSemicolomText());
            for (int i = 0; i < editTexts.size(); i++) {
                editTexts.get(i).setText(ips[i]);
            }
        }
    }

    public String getText() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < editTexts.size(); i++) {
            String ip = editTexts.get(i).getText().toString();
            if (!TextUtils.isEmpty(ip)) {
                sb.append(ip);
                if (i != editTexts.size() -1) {
                    sb.append(getSemicolomText());
                }
            }
        }
        return sb.toString();
    }

    public void clear() {
        for (int i = 0; i < editTexts.size(); i++) {
            editTexts.get(i).setText("");
        }
    }

    class OnDelKeyListener implements View.OnKeyListener {

        private AbsEditText clearEditText;
        private AbsEditText requestEditText;

        public OnDelKeyListener(AbsEditText requestEditText, AbsEditText clearEditText){
            this.requestEditText = requestEditText;
            this.clearEditText = clearEditText;
        }
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_DEL
                    && event.getAction() == KeyEvent.ACTION_DOWN
                    && clearEditText.getSelectionStart() == 0) {
                clearEditText.clearFocus();
                requestEditText.requestFocus();
                requestEditText.setSelection(requestEditText.length());
                return true;
            }
            return false;
        }
    }

    public abstract int getChildCount();

    public abstract AbsEditText getAbsEditText();

    public abstract String getSemicolomText();

    public abstract int getDelMaxLength();

    public abstract void applySemicolonTextViewTheme(TextView semicolonTextView);

    public abstract void applyEditTextTheme(AbsEditText absEditText);


}

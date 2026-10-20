package com.example.targetapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView tv = new TextView(this);
        tv.setText(getMessage());
        tv.setTextSize(24);
        tv.setPadding(40, 40, 40, 40);
        setContentView(tv);
    }

    /**
     * 这个方法会被 lsp-01 hook
     */
    public String getMessage() {
        return "原始消息";
    }
}

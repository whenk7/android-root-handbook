package com.example.lsp01;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * 模块自己的 UI（可选）——告诉用户这个模块在干啥
 */
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        TextView tv = new TextView(this);
        tv.setText("这是 LSP01 模块\n\n启用后：\n1. 在 LSPosed Manager 里启用本模块\n2. 勾选作用域 com.example.targetapp\n3. 强停目标 App\n4. 重新打开，看效果");
        tv.setPadding(40, 40, 40, 40);
        setContentView(tv);
    }
}

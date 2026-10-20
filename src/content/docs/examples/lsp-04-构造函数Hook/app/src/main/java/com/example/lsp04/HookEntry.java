package com.example.lsp04;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String TAG = "MyLSP04";
    private static final String TARGET_PACKAGE = "com.example.targetapp";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) return;

        XposedBridge.log(TAG + ": hook 目标 App 的构造函数");

        // hook User 类的构造函数（带 String 参数）
        XposedHelpers.findAndHookConstructor(
            "com.example.targetapp.User",
            lpparam.classLoader,
            String.class,                          // 构造参数：String name
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // param.thisObject 是刚构造出来的对象
                    Object user = param.thisObject;
                    
                    // 改它的 name 字段
                    XposedHelpers.setObjectField(user, "name", "Hacker");
                    
                    XposedBridge.log(TAG + ": User 对象已被改，name=Hacker");
                }
            }
        );
    }
}

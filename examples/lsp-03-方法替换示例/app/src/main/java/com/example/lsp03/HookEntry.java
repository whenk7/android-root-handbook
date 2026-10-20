package com.example.lsp03;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String TAG = "MyLSP03";
    private static final String TARGET_PACKAGE = "com.example.targetapp";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) return;

        XposedBridge.log(TAG + ": hook 目标 App");

        // 用 XC_MethodReplacement 完全替换 isVip 方法
        XposedHelpers.findAndHookMethod(
            "com.example.targetapp.MainActivity",
            lpparam.classLoader,
            "isVip",
            new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log(TAG + ": isVip 被替换，永远返回 true");
                    return true;   // 完全替换，原方法不执行
                }
            }
        );
    }
}

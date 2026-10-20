package com.example.lsp01;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String TAG = "MyLSP01";
    private static final String TARGET_PACKAGE = "com.example.targetapp";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 只处理目标 App
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) return;

        XposedBridge.log(TAG + ": 目标 App 加载了，pid=" + android.os.Process.myPid());

        // hook getMessage 方法
        XposedHelpers.findAndHookMethod(
            "com.example.targetapp.MainActivity",   // 类名
            lpparam.classLoader,                     // ClassLoader
            "getMessage",                            // 方法名
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    XposedBridge.log(TAG + ": getMessage 被调用了");
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    // 改返回值
                    param.setResult("被 hook 了！");
                    XposedBridge.log(TAG + ": getMessage 返回值已替换");
                }
            }
        );
    }
}

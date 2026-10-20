package com.example.lsp05;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String TAG = "MyLSP05";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        switch (lpparam.packageName) {
            case "com.tencent.mm":
                hookWechat(lpparam);
                break;
            case "com.tencent.mobileqq":
                hookQQ(lpparam);
                break;
            case "tv.danmaku.bili":
                hookBili(lpparam);
                break;
        }
    }

    private void hookWechat(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log(TAG + ": 微信加载了");
        // 这里写微信的 hook 逻辑
    }

    private void hookQQ(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log(TAG + ": QQ 加载了");
        // 这里写 QQ 的 hook 逻辑
    }

    private void hookBili(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log(TAG + ": B 站加载了");
        // 这里写 B 站的 hook 逻辑
    }
}

package com.example.lsp06;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 微信防撤回
 * 
 * ⚠️ 这是示例代码，实际类名/方法名每个版本都不一样
 * 你需要先用 JADX 反编译你当前的微信，找到真实的类名和方法名
 */
public class HookEntry implements IXposedHookLoadPackage {

    private static final String TAG = "AntiRevoke";
    private static final String TARGET_PACKAGE = "com.tencent.mm";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) return;

        XposedBridge.log(TAG + ": 微信加载了，pid=" + android.os.Process.myPid());

        // 注意：下面的类名/方法名是占位符，需要替换成你当前微信的真实名字
        hookRevokeHandler(lpparam);
        hookNotificationRevoke(lpparam);
    }

    /**
     * Hook 消息撤回处理类（占位符）
     * 
     * 真实情况可能是：
     * - com.tencent.mm.modelmsg.XXX
     * - com.tencent.mm.storage.XXX
     * - com.tencent.mm.modelsimple.XXX
     */
    private void hookRevokeHandler(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.tencent.mm.modelmsg.MsgRevokeHandler",  // ← 占位符，要改
                lpparam.classLoader,
                "onRevoke",                                   // ← 占位符，要改
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        // 直接吃掉这个调用，不执行原方法
                        param.setResult(null);
                        XposedBridge.log(TAG + ": 拦截了一次撤回");
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": hookRevokeHandler 失败: " + t);
        }
    }

    /**
     * Hook 通知栏的撤回提示
     * 让"对方撤回了一条消息"不显示
     */
    private void hookNotificationRevoke(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // 微信的"撤回"提示也是一条消息，类型是 10000 之类的
            // 可以 hook 消息显示方法，判断类型跳过
            XposedHelpers.findAndHookMethod(
                "com.tencent.mm.ui.chatting.ChattingUI",  // ← 占位符
                lpparam.classLoader,
                "addMessage",                              // ← 占位符
                Object.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Object msg = param.args[0];
                        // 判断消息类型，如果是"撤回提示"类型就不显示
                        // int type = XposedHelpers.getIntField(msg, "field_type");
                        // if (type == 10000) param.setResult(null);
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": hookNotificationRevoke 失败: " + t);
        }
    }
}

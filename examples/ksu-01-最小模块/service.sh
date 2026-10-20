#!/system/bin/sh
# MODDIR = 模块所在目录（自动）
MODDIR=${0%/*}

# 等系统启动完
until [ "$(getprop sys.boot_completed)" = "1" ]; do
    sleep 1
done

# 写一行日志
echo "$(date '+%Y-%m-%d %H:%M:%S'): Hello from my first module" >> /data/local/tmp/my_first_module.log

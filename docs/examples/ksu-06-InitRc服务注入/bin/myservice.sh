#!/system/bin/sh
# 服务脚本：系统启动完成后跑一次

LOG=/data/local/tmp/myservice.log

echo "$(date '+%Y-%m-%d %H:%M:%S'): myservice started" >> $LOG

# 干点事
echo "  - Android version: $(getprop ro.build.version.release)" >> $LOG
echo "  - Kernel: $(uname -r)" >> $LOG
echo "  - Device: $(getprop ro.product.model)" >> $LOG

echo "$(date '+%Y-%m-%d %H:%M:%S'): myservice done" >> $LOG

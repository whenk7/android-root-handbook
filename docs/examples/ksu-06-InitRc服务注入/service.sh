#!/system/bin/sh
MODDIR=${0%/*}

# 双保险：initrc 注入失败的话，service.sh 也能跑一次
until [ "$(getprop sys.boot_completed)" = "1" ]; do
    sleep 1
done

# 如果 initrc 没启动，这里手动启动
if ! pgrep -f "myservice.sh" > /dev/null; then
    nohup $MODDIR/bin/myservice.sh > /dev/null 2>&1 &
fi

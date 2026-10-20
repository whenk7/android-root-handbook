#!/system/bin/sh
MODDIR=${0%/*}

# 等系统启动完
until [ "$(getprop sys.boot_completed)" = "1" ]; do
    sleep 1
done

# 启动 daemon（后台）
nohup $MODDIR/bin/heartbeat.sh > /dev/null 2>&1 &

echo "$(date '+%Y-%m-%d %H:%M:%S'): daemon started" >> /data/local/tmp/heartbeat.log

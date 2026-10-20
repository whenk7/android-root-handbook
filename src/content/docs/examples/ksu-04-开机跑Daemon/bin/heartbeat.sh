#!/system/bin/sh
# 心跳 daemon：每秒往日志写一行

LOG=/data/local/tmp/heartbeat.log

# 主循环
while true; do
    echo "$(date '+%Y-%m-%d %H:%M:%S') alive" >> $LOG
    sleep 1
done

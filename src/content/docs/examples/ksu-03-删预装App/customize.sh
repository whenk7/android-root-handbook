#!/system/bin/sh

# 在这里列出要删除的路径
# KSU 安装时会自动 mknod c 0:0

REMOVE="
/system/app/YouTube
/system/app/Gmail
/system/app/Maps
"

# 注意：
# - 路径必须是绝对路径
# - 一个一行
# - 别乱删系统关键 App

ui_print "将删除以下预装 App:"
echo "$REMOVE" | while read line; do
    [ -n "$line" ] && ui_print "  - $line"
done

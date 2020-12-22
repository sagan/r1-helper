setlocal
cd /d %~dp0

adb push app-debug.apk /data/local/tmp
adb shell /system/bin/pm install -t -r /data/local/tmp/app-debug.apk
adb shell am start -n me.sagan.r1helper/.MainActivity

pause
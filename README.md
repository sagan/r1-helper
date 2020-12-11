

# R1Helper / R1 助手

适用于斐讯 R1 音箱的 Amazon Alexa 语音助手 app。基于 [AlexaAndroid](https://github.com/willblaschko/AlexaAndroid) 这个框架开发。语音唤醒("Alexa")使用的是 [Snowboy](https://snowboy.kitt.ai/)。目前只支持英语。

Target SDK level: 22 (Android 5.1) (ARMv7)

Package: me.sagan.r1helper

本 app 部分功能需要 root 权限。

## 构建

首先把你自己的 Android app 签名证书放到项目根目录的 key.jks 文件。然后修改 app/build.gradle 里的证书信息（证书名、密码）为你自己的：

```
android {
    signingConfigs {
        release {
            keyAlias 'key0'
            keyPassword '123456'
            storeFile file('../key.jks')
            storePassword '123456'
        }
    }
}
```

然后请参考amazon alexa service的[官方开发文档](https://developer.amazon.com/en-US/docs/alexa/alexa-voice-service/get-started-with-alexa-voice-service.html)，在 [amazon alexa voice service 控制台](https://developer.amazon.com/alexa/console/avs/products) 创建一个新的 product 和 security profile。注意创建 security profile 时需要在页面上填入你的 app 包名和 app 签名证书的 md5 / sha256。

把创建的 Security Profile 里的 Android API key 保存为并替换 app/src/main/assets/api_key.txt 文件。然后修改 app/src/main/res/values/strings.xml 里 name="alexa_product_id" 的字符串为你创建的 amazon alexa voice service product 的 id。

最后使用 Android Studio 构建即可。

## 安装

进行以下操作之前，请首先给R1音箱配网。（因为禁用了系统 app 后就无法通过长按顶部按键进入配网模式了）

首先用 adb 禁用 R1 自带的以下系统 app :

```
adb shell /system/bin/pm hide com.phicomm.speaker.player
adb shell /system/bin/pm hide com.phicomm.speaker.device
adb shell /system/bin/pm hide com.phicomm.speaker.airskill
adb shell /system/bin/pm hide com.phicomm.speaker.exceptionreporter
adb shell /system/bin/pm hide com.phicomm.speaker.ijetty
adb shell /system/bin/pm hide com.phicomm.speaker.netctl
adb shell /system/bin/pm hide com.phicomm.speaker.otaservice
adb shell /system/bin/pm hide com.phicomm.speaker.systemtool
adb shell /system/bin/pm hide com.phicomm.speaker.productiontest
adb shell /system/bin/pm hide com.phicomm.speaker.bugreport
```

其中最关键的是禁用掉 com.phicomm.speaker.player (EchoService) 和 com.phicomm.speaker.device (Unisound) 这两个。注意不要禁用 com.phicomm.speaker.launcher。

安装本 app 并立即启动

```
adb push app-debug.apk /data/local/tmp
adb shell /system/bin/pm install -t -r /data/local/tmp/app-debug.apk
adb shell /system/bin/am start -n me.sagan.r1helper/.MainActivity
```

启动时，本 app 会尝试请求 root 权限。如果 R1 没有 root，那么本 app 控制LED灯等部分功能不可用（详见下文）。

下次开机时，本 app 会自启动。

## 使用

### Alexa 语音助手

使用 Alexa 语音助手前，需要在 R1 设备上登录 amazon 帐号。首先还需要在 R1 上安装任意一个浏览器，测试用 [Google Chrome 46.0.2490.76 (arm-v7a)](https://www.apkmirror.com/apk/google-inc/chrome/chrome-46-0-2490-76-release/chrome-46-0-2490-76-android-5-0-android-apk-download/) 可以。然后用 adb 投屏工具 (如 [scrcpy](https://github.com/Genymobile/scrcpy))连接 R1，启动本程序，点击 "LOGIN" 按钮，然后在打开的浏览器里登录 amazon 帐号并授权本应用即可。

使用 "Alexa" ([读音](https://www.youtube.com/watch?v=U9N1xpcWwD0)) 唤醒语音助手。如果想修改唤醒词，可以尝试在 snowboy 网站上生成自己的唤醒词语音模型，然后放到 R1 设备上替换 /sdcard/r1helper/alexa.umdl 文件。修改后需要彻底停止本 app 然后重新启动以生效：

```
adb shell am force-stop me.sagan.r1helper
adb shell am start -n me.sagan.r1helper/.MainActivity
```

目前支持用英语与 Alexa 对话。

### 配置 Alexa 语音助手

在PC上打开 [Amazon alexa home](https://alexa.amazon.com/) 或在手机上安装 Amazon alexa app，用你的 amazon 账号登录，在 Settings 页面找到 "R1-assistant" 设备，里面可以配置设备的地理位置、时区等。

### 蓝牙配对・LED 灯

本 App 自带控制 R1 蓝牙模式控制、LED 灯控制功能。  开机时自动打开蓝牙。但控制 LED 灯需要 root 权限。

按 R1 顶部中央的按键可以在以下几种模式之间切换：

* LED 灯常亮：
  * 播放音频时，显示氛围灯效果。
  * 未播放音频时，根据当前时间在 0-24 点之间显示HSL光谱里不同的颜色。如 0 点显示红色。中午12点显示蓝色。
* 关闭 LED 灯。
* 蓝牙配对模式：此模式下允许配对新的蓝牙设备。LED 灯交替闪烁蓝、白色。

如果没有 root，那么任何模式下 LED 灯都不会亮。

补充说明：

R1 控制 LED 灯方式本质上是向 /sys/class/leds/multi_leds0/led_color 这个设备文件写入11字节的数据。例如：

adb shell "echo -n '7fff ffffff' > /sys/class/leds/multi_leds0/led_color"

其中 ffffff 是 RGB 颜色数值（ 000000 为 关闭 LED 灯）。但由于 selinux 限制，第三方 app 无法访问这个设备文件，除非获得 root 权限后关闭 selinux。
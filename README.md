
# R1Helper / R1 助手

适用于斐讯 R1 音箱的 Amazon Alexa 语音助手 app。基于 [AlexaAndroid](https://github.com/willblaschko/AlexaAndroid) 这个框架开发。语音唤醒("Alexa")使用的是 [Snowboy](https://snowboy.kitt.ai/)。

Target SDK level: 22 (Android 5.1) (ARMv7)

Package: me.sagan.r1helper

## 构建

首先把你自己的 Android app 签名证书放到项目根目录的 key.jks 文件。然后修改 app/build.gradle 里的证书信息（证书名密码）为你自己的：

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

把 Security Profile 里的 Android API key 保存并替换 app/src/main/assets/api_key.txt 文件。然后修改 app/src/main/res/values/strings.xml 里 name="alexa_product_id" 的字符串为你创建的 amazon alexa voice service product 的 id。

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

下次开机时，本 app 会自启动。

## 使用

### Alexa 语音助手

使用 Alexa 语音助手前，需要在 R1 设备上登录 amazon 帐号。首先需要在 R1 上安装任意一个浏览器，测试用 [Google Chrome 46.0.2490.76 (arm-v7a)](https://www.apkmirror.com/apk/google-inc/chrome/chrome-46-0-2490-76-release/chrome-46-0-2490-76-android-5-0-android-apk-download/) 可以。然后用 adb 投屏工具 (如 [scrcpy](https://github.com/Genymobile/scrcpy))连接 R1，启动本程序，点击 "LOGIN" 按钮，然后在打开的浏览器里登录 amazon 帐号并授权本应用即可。

使用 "Alexa" ([读音](https://www.youtube.com/watch?v=U9N1xpcWwD0)) 唤醒语音助手。如果想修改唤醒词，可以尝试在 snowboy 网站上生成自己的唤醒词语音模型，然后放到 R1 设备上替换 /sdcard/r1helper/alexa.umdl 文件。修改后需要彻底停止本 app 然后重新启动以生效：

```
adb shell am force-stop me.sagan.r1helper
adb shell am start -n me.sagan.r1helper/.MainActivity
```

目前支持用英语与 Alexa 对话。

### LED 灯

本 App 自带 R1 的底部 LED 灯控制功能。

* 播放音频时，显示氛围灯效果。
* 未播放音频时，根据当前时间在 0-24 点之间显示HSL光谱里不同的颜色。如 0 点显示红色。中午12点显示蓝色。

### 蓝牙配对

本 App 自带控制 R1 蓝牙模式功能。开机时自动打开蓝牙。按 R1 顶部中央的按键可以在以下几种模式之间切换：

* 正常模式（LED 灯常亮）。
* 关闭 LED 灯。
* 蓝牙配对模式：此模式下允许配对新的蓝牙设备。LED 灯交替闪烁蓝、白色。


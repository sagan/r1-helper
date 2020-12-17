

# R1Helper / R1 助手

适用于斐讯 R1 音箱 (Phicomm R1 speaker) 的 Amazon Alexa 语音助手 app。基于 [AlexaAndroid](https://github.com/willblaschko/AlexaAndroid) 框架开发（这个框架使用了 [Alexa Voice Service (HTTP) API](https://developer.amazon.com/en-US/docs/alexa/alexa-voice-service/api-overview.html)）。语音唤醒 ("Alexa") 使用的是 [Snowboy](https://snowboy.kitt.ai/)。

* Target SDK level: 22 (Android 5.1) (ARMv7 only)
* Package: me.sagan.r1helper
* 本 app 可以安装运行在原厂固件的 R1 设备上。但如果想控制 R1 的底部 LED 灯，设备必须已经 root（需要拆机焊接 usb 并刷机）。

请注意：本语音助手不支持中文。Amazon alexa 目前支持英语、日语、法语、德语、葡萄牙语和意大利语等语言。默认使用英语。

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

启动时，本 app 会尝试请求 root 权限。如果 R1 没有 root，那么本 app 控制 LED 灯等部分功能不可用（详见下文）。

下次开机时，本 app 会自启动。

## 使用

### Alexa 语音助手

使用 Alexa 语音助手前，需要在 R1 设备上登录 amazon 帐号。首先还需要在 R1 上安装任意一个浏览器，测试用 [Google Chrome 46.0.2490.76 (arm-v7a)](https://www.apkmirror.com/apk/google-inc/chrome/chrome-46-0-2490-76-release/chrome-46-0-2490-76-android-5-0-android-apk-download/) 可以。然后用 adb 投屏工具（如 [scrcpy](https://github.com/Genymobile/scrcpy)）连接 R1，启动本程序，点击 "LOGIN" 按钮，然后在打开的浏览器里登录 amazon 帐号并授权本应用即可。

使用 "Alexa" ([读音](https://www.youtube.com/watch?v=U9N1xpcWwD0)) 唤醒语音助手。如果想修改唤醒词，可以尝试在 snowboy 网站上生成自己的唤醒词语音模型，放到 R1 设备上 /sdcard/r1helper/ 目录下，然后修改本 app 的 snowboy_model 配置为你的模型文件名（参考下文）。

```
adb shell am force-stop me.sagan.r1helper
adb shell am start -n me.sagan.r1helper/.MainActivity
```

### 蓝牙配对・LED 灯控制

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


## 配置

### 修改 Alexa 助手语言

ALexa 语音助手默认使用英文。如果想要修改 Alexa 语言，可以用 adb 投屏工具打开本 app，（在已登录 amazon 帐号状态下）点击 "JA" 按钮切换语音助手语言为日语；再次点击 "EN" 按钮可以切换回英语。

如果想要修改为其他 Alexa 支持的语言，请参考下面“HTTP 接口”部分。

### HTTP 接口

本 app 会在 tcp/9000 端口启动一个 http 服务器，通过这个 http 接口可以对本 app 和 R1 设备进行一些控制：

HTTP API 列表：

* GET /reboot : 重启设备。（需要 root 权限）
* GET /run?cmd=pwd : 在设备上运行一个命令并返回结果。（需要 root 权限）
* GET /status : 返回 app 运行状态、配置、最近日志等信息。
* GET /log : 返回 app 运行日志。
* GET /set?lang=ja-JP : 更改设备设定。可选参数：
    * lang : 设置 Alexa 语音助手使用的语言。可选的 lang 语言值包括：de-DE, en-AU, en-CA, en-GB, en-IN, en-US, es-ES, es-MX, fr-CA, fr-FR, it-IT, ja-JP。
    * mode : 更改 app 运行模式。mode 值：0 - 正常模式(显示 LED 灯和氛围灯效果); 1 - 关闭 LED 灯; 2 - 蓝牙配对模式 (LED灯交替闪烁蓝、白色)。
* GET /config?sensitivity=0.3 : 获取或修改 app (持久化)首选项参数。可选参数：
    * sensitivity : 语音助手唤醒词识别敏感度。范围 [0,1]。数值越大则越容易唤醒，但误唤醒率也会更高。
    * recordPausing : 毫秒数。> 0 则启用录音时本地语音结束检测（录音至少需已开始这么长时间才可能结束）。不推荐。Alexa服务器云端会自动检测语音结束。
    * snowboy_model : 唤醒词检测使用的 snowboy 模型文件名。默认为 alexa.umdl。模型文件存放路径 /sdcard/r1helper。
    * setpassword : 设置 HTTP API 的密码。
* GET /reset : 重置 Alexa 语音助手状态。如果语音助手一直没反应或不听使唤，可以尝试重置。可选参数 full=1 彻底重启 alexa 所有服务。

HTTP API 默认无需验证，使用 GET /config?setpassword=123 接口可以设置密码。设置密码以后，以上所有 API 访问时都必须带上 password 参数提供当前密码。

如果忘记了设置的密码，可以用 adb 清除本 app 全部数据以恢复初始状态：

```
adb shell /system/bin/pm clear me.sagan.r1helper
```

或卸载本 app 然后重新安装一次亦可。

```
adb shell /system/bin/pm uninstall me.sagan.r1helper
adb shell /system/bin/pm install -t -r /data/local/tmp/app-debug.apk
```


### 配置设备地理位置 / 时区

如果想用语音向 Alexa 查询天气、时间，需要先配置设备相关信息。

在PC上打开 [Amazon alexa home](https://alexa.amazon.com/) 或在手机上安装 Amazon alexa app，用你的 amazon 账号登录，在 Settings 页面找到 "yourname 's R1-assistant" 设备（此设备即为你的安装了本 app 的设备），里面可以配置设备的地理位置、时区等信息。

## 其他说明

### 本 app Alexa 语音助手的局限性

由于 Amazon 的[官方限制](https://developer.amazon.com/ja-JP/docs/alexa/alexa-voice-service/music-service-providers.html#testing)，本 app 的 alexa语音助手目前无法播放来自 Amazon Music，Audible 等音乐和媒体内容。
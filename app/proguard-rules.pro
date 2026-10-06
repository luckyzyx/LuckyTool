# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

#忽略警告
-ignorewarnings
#不做预校验，preverify是proguard的四个步骤之一，Android不需要preverify，去掉这一步能够加快混淆速度。
-dontpreverify

#指定代码优化级别，值在0-7之间，默认为5
-optimizationpasses 7
#混淆时不使用大小写混合类名
#-dontusemixedcaseclassnames

#关闭代码优化，会使apk体积增大
#-dontoptimize

# 有了verbose这句话，混淆后就会生成映射文件
# 包含有类名->混淆后类名的映射关系
# 然后使用printmapping指定映射文件的名称
-verbose
-printmapping proguardMapping.txt

# 指定混淆时采用的算法，后面的参数是一个过滤器
# 这个过滤器是谷歌推荐的算法，一般不改变
#-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

#-adaptclassstrings
#-adaptresourcefilenames
#-adaptresourcefilecontents

#将文件来源重命名为“SourceFile”字符串
-renamesourcefileattribute SourceFile

#抛出异常时保留代码行号，在异常分析中可以方便定位
#-keepattributes SourceFile,LineNumberTable

#优化时允许访问并修改有修饰符的类和类的成员，这可以提高优化步骤的结果。
# 比如，当内联一个公共的getter方法时，这也可能需要外地公共访问。
# 虽然java二进制规范不需要这个，要不然有的虚拟机处理这些代码会有问题。当有优化和使用-repackageclasses时才适用。
# 指示语：不能用这个指令处理库中的代码，因为有的类和类成员没有设计成public ,而在api中可能变成public
-allowaccessmodification
#当有优化和使用-repackageclasses时才适用。
-repackageclasses
#混淆时应用侵入式重载
-overloadaggressively

# JSON解析异常处理
# 保护代码中的Annotation不被混淆
# 这在JSON实体映射时非常重要，比如fastJson
-keepattributes *Annotation*
# 避免混淆泛型
# 这在JSON实体映射时非常重要，比如fastJson
-keepattributes Signature

#指定外部模糊字典
-obfuscationdictionary ../keystore/proguard-custom.txt
#指定class模糊字典
-classobfuscationdictionary ../keystore/proguard-custom.txt
#指定package模糊字典
-packageobfuscationdictionary ../keystore/proguard-custom.txt

# 崩溃日志手机端显示
-keep class com.simple.spiderman.** { *; }
-keepnames class com.simple.spiderman.** { *; }
-keep public class * extends android.app.Activity
-keep class * implements Android.os.Parcelable {
    public static final Android.os.Parcelable$Creator *;
}
# androidx
-keep public class * extends androidx.annotation.** { *; }
-keep public class * extends androidx.core.content.FileProvider

# BetterAndroid 使用了反射功能来装载 ViewBinding，所以你需要确保 ViewBinding 相关功能不被混淆，否则会无法装载
# 如果你引用了 ui-component 或 ui-extension 相关功能，请添加以下混淆规则
-keep class * extends android.app.Activity
-keep class * implements androidx.viewbinding.ViewBinding {
    <init>();
    *** bind(***);
    *** inflate(...);
}

#-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
#    public static void check*(...);
#    public static void throw*(...);
#}
#-assumenosideeffects class java.util.Objects {
#    ** requireNonNull(...);
#}

-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}
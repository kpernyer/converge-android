# Converge Android ProGuard Rules

# gRPC
-keep class io.grpc.** { *; }
-keep class com.google.protobuf.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep generated gRPC stubs
-keep class zone.converge.android.grpc.** { *; }

-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

-keepclassmembers class ** implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

-dontwarn coil.**

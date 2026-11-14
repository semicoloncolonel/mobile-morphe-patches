package semicoloncolonel.mobilestream.patches.netflix.bypass

import app.morphe.patcher.Fingerprint

// Netflix's Application subclass. On 9.93.0 (vc 74878) R8 renamed it to
// Lo/gLY; (super Lo/gLQ;); it is the class named in AndroidManifest's
// <application android:name>. Its attachBaseContext(Context) runs before
// onCreate and before the native tamper guard arms, so it is the earliest
// Java site at which to load libnfkill.so. The name rotates per build (it is
// derived from a build hash), so re-locate it when bumping the target: it is
// the manifest application class whose attachBaseContext calls invoke-super
// to its own parent, not android.app.Application directly.
internal object ApplicationAttachBaseContextFingerprint : Fingerprint(
    definingClass = "Lo/gLY;",
    name = "attachBaseContext",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
)

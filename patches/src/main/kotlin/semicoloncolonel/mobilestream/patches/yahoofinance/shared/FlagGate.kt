package semicoloncolonel.mobilestream.patches.yahoofinance.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.BytecodePatchContext
import semicoloncolonel.mobilestream.patches.shared.misc.prependGate

// ─────────────────────────────────────────────────────────────────────────────
// Yahoo Finance 26.22.2 reads all 293 feature flags through one class,
// config/c: a() is the value with any override applied, and c() the value
// itself. The flag's key is the string in field a. offFlags puts a check at
// the top of both: when FinanceFlags.<check>(key) says the flag is on the
// patch's list, the flag reads as off.
// ─────────────────────────────────────────────────────────────────────────────

internal const val FINANCE_FLAGS = "Lsemicoloncolonel/mobilestream/extension/yahoofinance/FinanceFlags;"
private const val FLAG = "Lcom/yahoo/mobile/client/android/finance/config/c;"

internal object FlagValueFingerprint : Fingerprint(
    definingClass = FLAG,
    name = "a",
    parameters = listOf(),
    returnType = "Z",
)

internal object FlagServerValueFingerprint : Fingerprint(
    definingClass = FLAG,
    name = "c",
    parameters = listOf(),
    returnType = "Z",
)

internal fun BytecodePatchContext.offFlags(check: String) {
    listOf(FlagValueFingerprint, FlagServerValueFingerprint).forEach {
        it.method.prependGate(
            """
                iget-object v0, p0, $FLAG->a:Ljava/lang/String;
                invoke-static { v0 }, $FINANCE_FLAGS->$check(Ljava/lang/String;)Z
                move-result v0
                if-eqz v0, :msp_orig
                const/4 v0, 0x0
                return v0
            """,
        )
    }
}

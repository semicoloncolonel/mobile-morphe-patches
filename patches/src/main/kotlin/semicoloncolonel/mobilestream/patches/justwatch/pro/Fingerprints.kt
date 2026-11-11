package semicoloncolonel.mobilestream.patches.justwatch.pro

import app.morphe.patcher.Fingerprint

private const val ENTITLEMENT_INFOS = "Lcom/revenuecat/purchases/EntitlementInfos;"

object GetActiveFingerprint : Fingerprint(
    definingClass = ENTITLEMENT_INFOS,
    name = "getActive",
    returnType = "Ljava/util/Map;",
)

object GetAllFingerprint : Fingerprint(
    definingClass = ENTITLEMENT_INFOS,
    name = "getAll",
    returnType = "Ljava/util/Map;",
)

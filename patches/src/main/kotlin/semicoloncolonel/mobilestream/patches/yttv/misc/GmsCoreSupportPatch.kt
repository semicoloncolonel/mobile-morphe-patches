/*
 * Ported from Morphe's "GmsCore support" patch (GPL-3.0):
 * https://github.com/MorpheApp/morphe-patches/tree/main/patches/src/main/kotlin/app/morphe/patches/shared/misc/gms
 * with the package rename from Morphe's "Clone app" and the manifest queries
 * from morphe-patches-library's gmsCorePackageQueriesPatch.
 * Copyright 2026 Morphe. Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 * See the NOTICE file for the GPLv3 Section 7 terms that apply to Morphe contributions.
 * ALL CREDIT FOR THE GMSCORE SUPPORT DESIGN GOES TO MORPHE AND REVANCED.
 */

package semicoloncolonel.mobilestream.patches.yttv.misc

import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.InstallerType
import app.morphe.patcher.patch.Option
import app.morphe.patcher.patch.PatchAvailability
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.string
import app.morphe.patcher.util.matchAllMethodIndicesForEach
import app.morphe.patcher.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import semicoloncolonel.mobilestream.patches.yttv.shared.Constants

// YouTube TV signs in with the Google account on the device through Google
// Play services, which only answers apps signed by Google. A re-signed copy
// has to use GmsCore (ReVanced's microG build, package
// app.revanced.android.gms) instead. This does what Morphe's patch does for
// YouTube:
//
// - The app gets its own package name, so it installs beside the stock app.
// - Every Google Play services package name, permission, intent action, and
//   content provider authority in the app and its manifest points at GmsCore.
// - The manifest tells GmsCore which package name and signing certificate to
//   present to Google, so Google sees the stock app.
// - The Play services availability checks are skipped, and MainActivity asks
//   the extension to check that GmsCore is installed and may run in the
//   background.
//
// Morphe's settings screen entries, its MicroG-RE conflict and update checks,
// and the YouTube-only parts (spoofed video streams, the cast button, the
// device compliance check) are left out. YouTube TV has no settings screen
// for them, and the conflict check would flag ReVanced's GmsCore, which is
// signed with a different key than MicroG-RE.

private const val FROM_PACKAGE_NAME = Constants.PACKAGE_NAME
private const val TO_PACKAGE_NAME_DEFAULT = "app.morphe.android.youtube.unplugged"

// Old vendor id, which ReVanced's GmsCore and MicroG-RE both still use.
private const val GMS_CORE_VENDOR_GROUP_ID = "app.revanced"
private const val LOCATION_SERVICE_ACTION = "com.google.android.location.internal.GoogleLocationManagerService"

// SHA-1 of YouTube TV's original signing certificate
// (CN=youtube_android_unplugged). 10.44.0 is signed with a rotated key for
// Android 13 and up, but Google's sign-in still identifies the app by this one.
private const val SPOOFED_PACKAGE_SIGNATURE = "3a82b5ee26bc46bf68113d920e610cd090198d4a"

private lateinit var packageNameOption: Option<String>

private val toPackageName get() = packageNameOption.value ?: TO_PACKAGE_NAME_DEFAULT

private val gmsCoreSupportResourcePatch = resourcePatch {
    execute {
        val toPackageName = toPackageName

        // Text replacements, as in Morphe. These also rename the manifest
        // package (what "Clone app" does) and the app's own provider
        // authorities and permissions, so the patched app does not conflict
        // with the stock app.
        val transformations = mapOf(
            "package=\"$FROM_PACKAGE_NAME" to "package=\"$toPackageName",
            "android:authorities=\"$FROM_PACKAGE_NAME" to "android:authorities=\"$toPackageName",
            "$FROM_PACKAGE_NAME.permission.C2D_MESSAGE" to "$toPackageName.permission.C2D_MESSAGE",
            "$FROM_PACKAGE_NAME.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" to
                "$toPackageName.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
            "com.google.android.c2dm" to "$GMS_CORE_VENDOR_GROUP_ID.android.c2dm",
        )
        val manifest = get("AndroidManifest.xml")
        manifest.writeText(
            transformations.entries.fold(manifest.readText()) { text, (from, to) -> text.replace(from, to) }
        )

        document("AndroidManifest.xml").use { document ->
            val root = document.documentElement
            if (root.getAttribute("package") != toPackageName) {
                throw PatchException("Manifest package was not renamed")
            }

            val application = document.getElementsByTagName("application").item(0)
            fun addMetaData(name: String, value: String) {
                application.appendChild(
                    document.createElement("meta-data").apply {
                        setAttribute("android:name", name)
                        setAttribute("android:value", value)
                    }
                )
            }
            // GmsCore presents this package name and signature to Google.
            addMetaData("$GMS_CORE_VENDOR_GROUP_ID.android.gms.SPOOFED_PACKAGE_NAME", FROM_PACKAGE_NAME)
            addMetaData("$GMS_CORE_VENDOR_GROUP_ID.android.gms.SPOOFED_PACKAGE_SIGNATURE", SPOOFED_PACKAGE_SIGNATURE)
            addMetaData("app.revanced.MICROG_PACKAGE_NAME", "$GMS_CORE_VENDOR_GROUP_ID.android.gms")

            // Package visibility, so the extension can see that GmsCore is
            // installed on Android 11 and up.
            val queries = document.getElementsByTagName("queries").item(0)
                ?: document.createElement("queries").also { root.appendChild(it) }
            queries.appendChild(
                document.createElement("package").apply {
                    setAttribute("android:name", "$GMS_CORE_VENDOR_GROUP_ID.android.gms")
                }
            )
        }
    }
}

@Suppress("unused")
val gmsCoreSupportPatch = bytecodePatch(
    name = "GmsCore support",
    description = "Lets the re-signed app sign in with a Google account through GmsCore (microG) " +
        "instead of Google Play services. The app is installed under a different package name, " +
        "so it can be installed beside the stock app. Requires ReVanced GmsCore.",
) {
    compatibleWith(Constants.COMPATIBILITY)

    // A root mount keeps the stock package name and Google's signature, so it
    // has no use for this patch. Every other install needs it.
    availability { installer, _ ->
        when (installer) {
            InstallerType.MOUNT -> PatchAvailability.UNAVAILABLE
            InstallerType.STANDARD, InstallerType.SHIZUKU -> PatchAvailability.REQUIRED
        }
    }

    packageNameOption = stringOption(
        key = "packageName",
        default = TO_PACKAGE_NAME_DEFAULT,
        values = mapOf("Default" to TO_PACKAGE_NAME_DEFAULT),
        title = "Package name",
        description = "Package name to install the patched app under.",
        required = true,
    ) {
        it!!.matches(Regex("^[a-z]\\w*(\\.[a-z]\\w*)+$")) && it != FROM_PACKAGE_NAME
    }

    dependsOn(gmsCoreSupportResourcePatch)

    extendWith("extensions/extension.mpe")

    execute {
        val toPackageName = toPackageName

        GooglePlayUtilityFingerprint.method.returnEarly(0)

        fun replaceStrings(fromString: String, toString: String) {
            string(fromString).matchAllMethodIndicesForEach(requireMatches = false) { index ->
                val register = getInstruction<OneRegisterInstruction>(index).registerA
                replaceInstruction(
                    index,
                    BuilderInstruction21c(Opcode.CONST_STRING, register, ImmutableStringReference(toString)),
                )
            }
        }

        arrayOf(
            "com.google" to GMS_CORE_VENDOR_GROUP_ID,
            "com.google.android.gms" to "$GMS_CORE_VENDOR_GROUP_ID.android.gms",
            "$FROM_PACKAGE_NAME.SuggestionProvider" to "$toPackageName.SuggestionProvider",
            "$FROM_PACKAGE_NAME.fileprovider" to "$toPackageName.fileprovider",
            // Morphe's list has this action without ".START", which YouTube TV
            // appends. Without it the fused location service cannot bind, and
            // YouTube TV cannot check the user's home area.
            "$LOCATION_SERVICE_ACTION.START" to
                "${LOCATION_SERVICE_ACTION.replace("com.google", GMS_CORE_VENDOR_GROUP_ID)}.START",
        ).forEach { (fromString, toString) -> replaceStrings(fromString, toString) }

        (GmsCoreConstants.PERMISSIONS + GmsCoreConstants.ACTIONS + GmsCoreConstants.AUTHORITIES).forEach {
            replaceStrings(it, it.replace("com.google", GMS_CORE_VENDOR_GROUP_ID))
        }

        // content:// URIs of the Play services providers.
        val authorityPrefixes = GmsCoreConstants.AUTHORITIES.map { it to "content://$it" }
        string("content://", StringComparisonType.STARTS_WITH).matchAllMethodIndicesForEach(
            requireMatches = false,
        ) { index ->
            val instruction = getInstruction<ReferenceInstruction>(index)
            val uri = (instruction.reference as StringReference).string
            val (authority, prefix) = authorityPrefixes.firstOrNull { uri.startsWith(it.second) }
                ?: return@matchAllMethodIndicesForEach
            replaceInstruction(
                index,
                BuilderInstruction21c(
                    Opcode.CONST_STRING,
                    (instruction as OneRegisterInstruction).registerA,
                    ImmutableStringReference(
                        uri.replace(prefix, "content://${authority.replace("com.google", GMS_CORE_VENDOR_GROUP_ID)}")
                    ),
                ),
            )
        }

        // Keep the renamed app on the Primes allowlist.
        PrimeMethodFingerprint.method.apply {
            val index = implementation!!.instructions.indexOfFirst {
                (it.opcode == Opcode.CONST_STRING || it.opcode == Opcode.CONST_STRING_JUMBO) &&
                    ((it as ReferenceInstruction).reference as StringReference).string == FROM_PACKAGE_NAME
            }
            if (index < 0) throw PatchException("Package name not found in the prime method")
            val register = getInstruction<OneRegisterInstruction>(index).registerA
            replaceInstruction(index, "const-string v$register, \"$toPackageName\"")
        }

        CastContextFetchFingerprint.method.returnEarly()
        ServiceCheckFingerprint.method.returnEarly()

        // Original package name and GmsCore vendor for the extension.
        OriginalPackageNameFingerprint.method.returnEarly(FROM_PACKAGE_NAME)
        GmsCoreVendorGroupIdFingerprint.method.returnEarly(GMS_CORE_VENDOR_GROUP_ID)

        // Check that GmsCore is installed and allowed to run in the background.
        MainActivityOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $GMS_EXTENSION_CLASS->checkGmsCore(Landroid/app/Activity;)V",
        )

        // GNP expects the original Google package even when the installed app is renamed.
        GnpRegistrationTargetFingerprint.apply {
            val result = instructionMatches[1]
            method.replaceInstruction(
                result.index,
                BuilderInstruction21c(
                    Opcode.CONST_STRING,
                    (result.instruction as OneRegisterInstruction).registerA,
                    ImmutableStringReference(FROM_PACKAGE_NAME),
                ),
            )
        }
    }
}

/**
 * Permissions, intent actions, and content provider authorities that GmsCore
 * declares under its own vendor id. Copied unchanged from Morphe.
 */
private object GmsCoreConstants {
    val PERMISSIONS = setOf(
        "com.google.android.c2dm.permission.RECEIVE",
        "com.google.android.c2dm.permission.SEND",
        "com.google.android.gms.auth.api.phone.permission.SEND",
        "com.google.android.gms.permission.AD_ID",
        "com.google.android.gms.permission.AD_ID_NOTIFICATION",
        "com.google.android.gms.permission.CAR_FUEL",
        "com.google.android.gms.permission.CAR_INFORMATION",
        "com.google.android.gms.permission.CAR_MILEAGE",
        "com.google.android.gms.permission.CAR_SPEED",
        "com.google.android.gms.permission.CAR_VENDOR_EXTENSION",
        "com.google.android.googleapps.permission.GOOGLE_AUTH",
        "com.google.android.googleapps.permission.GOOGLE_AUTH.cp",
        "com.google.android.googleapps.permission.GOOGLE_AUTH.local",
        "com.google.android.googleapps.permission.GOOGLE_AUTH.mail",
        "com.google.android.googleapps.permission.GOOGLE_AUTH.writely",
        "com.google.android.gtalkservice.permission.GTALK_SERVICE",
        "com.google.android.providers.gsf.permission.READ_GSERVICES",
    )

    val ACTIONS = setOf(
        "com.google.android.c2dm.intent.RECEIVE",
        "com.google.android.c2dm.intent.REGISTER",
        "com.google.android.c2dm.intent.UNREGISTER",
        "com.google.android.contextmanager.service.ContextManagerService.START",
        "com.google.android.gcm.intent.SEND",
        "com.google.android.gms.accounts.ACCOUNT_SERVICE",
        "com.google.android.gms.accountsettings.ACCOUNT_PREFERENCES_SETTINGS",
        "com.google.android.gms.accountsettings.action.BROWSE_SETTINGS",
        "com.google.android.gms.accountsettings.action.VIEW_SETTINGS",
        "com.google.android.gms.accountsettings.MY_ACCOUNT",
        "com.google.android.gms.accountsettings.PRIVACY_SETTINGS",
        "com.google.android.gms.accountsettings.SECURITY_SETTINGS",
        "com.google.android.gms.ads.gservice.START",
        "com.google.android.gms.ads.identifier.service.EVENT_ATTESTATION",
        "com.google.android.gms.ads.service.CACHE",
        "com.google.android.gms.ads.service.CONSENT_LOOKUP",
        "com.google.android.gms.ads.service.HTTP",
        "com.google.android.gms.analytics.service.START",
        "com.google.android.gms.appstate.service.START",
        "com.google.android.gms.appusage.service.START",
        "com.google.android.gms.asterism.service.START",
        "com.google.android.gms.audiomodem.service.AudioModemService.START",
        "com.google.android.gms.audit.service.START",
        "com.google.android.gms.auth.account.authapi.START",
        "com.google.android.gms.auth.account.authenticator.auto.service.START",
        "com.google.android.gms.auth.account.authenticator.chromeos.START",
        "com.google.android.gms.auth.account.authenticator.tv.service.START",
        "com.google.android.gms.auth.account.data.service.START",
        "com.google.android.gms.auth.api.credentials.PICKER",
        "com.google.android.gms.auth.api.credentials.service.START",
        "com.google.android.gms.auth.api.identity.service.authorization.START",
        "com.google.android.gms.auth.api.identity.service.credentialsaving.START",
        "com.google.android.gms.auth.api.identity.service.signin.START",
        "com.google.android.gms.auth.api.phone.service.InternalService.START",
        "com.google.android.gms.auth.api.signin.service.START",
        "com.google.android.gms.auth.be.appcert.AppCertService",
        "com.google.android.gms.auth.blockstore.service.START",
        "com.google.android.gms.auth.config.service.START",
        "com.google.android.gms.auth.cryptauth.cryptauthservice.START",
        "com.google.android.gms.auth.GOOGLE_SIGN_IN",
        "com.google.android.gms.auth.login.LOGIN",
        "com.google.android.gms.auth.proximity.devicesyncservice.START",
        "com.google.android.gms.auth.proximity.securechannelservice.START",
        "com.google.android.gms.auth.proximity.START",
        "com.google.android.gms.auth.service.START",
        "com.google.android.gms.backup.ACTION_BACKUP_SETTINGS",
        "com.google.android.gms.backup.G1_BACKUP",
        "com.google.android.gms.backup.G1_RESTORE",
        "com.google.android.gms.backup.GMS_MODULE_RESTORE",
        "com.google.android.gms.beacon.internal.IBleService.START",
        "com.google.android.gms.car.service.START",
        "com.google.android.gms.carrierauth.service.START",
        "com.google.android.gms.cast.firstparty.START",
        "com.google.android.gms.cast.remote_display.service.START",
        "com.google.android.gms.cast.service.BIND_CAST_DEVICE_CONTROLLER_SERVICE",
        "com.google.android.gms.cast_mirroring.service.START",
        "com.google.android.gms.checkin.BIND_TO_SERVICE",
        "com.google.android.gms.chromesync.service.START",
        "com.google.android.gms.clearcut.service.START",
        "com.google.android.gms.common.account.CHOOSE_ACCOUNT",
        "com.google.android.gms.common.download.START",
        "com.google.android.gms.common.service.START",
        "com.google.android.gms.common.telemetry.service.START",
        "com.google.android.gms.config.START",
        "com.google.android.gms.constellation.service.START",
        "com.google.android.gms.credential.manager.service.firstparty.START",
        "com.google.android.gms.deviceconnection.service.START",
        "com.google.android.gms.drive.ApiService.RESET_AFTER_BOOT",
        "com.google.android.gms.drive.ApiService.START",
        "com.google.android.gms.drive.ApiService.STOP",
        "com.google.android.gms.droidguard.service.INIT",
        "com.google.android.gms.droidguard.service.PING",
        "com.google.android.gms.droidguard.service.START",
        "com.google.android.gms.enterprise.loader.service.START",
        "com.google.android.gms.facs.cache.service.START",
        "com.google.android.gms.facs.internal.service.START",
        "com.google.android.gms.feedback.internal.IFeedbackService",
        "com.google.android.gms.fido.credentialstore.internal_service.START",
        "com.google.android.gms.fido.fido2.privileged.START",
        "com.google.android.gms.fido.fido2.regular.START",
        "com.google.android.gms.fido.fido2.zeroparty.START",
        "com.google.android.gms.fido.sourcedevice.service.START",
        "com.google.android.gms.fido.targetdevice.internal_service.START",
        "com.google.android.gms.fido.u2f.privileged.START",
        "com.google.android.gms.fido.u2f.thirdparty.START",
        "com.google.android.gms.fido.u2f.zeroparty.START",
        "com.google.android.gms.fitness.BleApi",
        "com.google.android.gms.fitness.ConfigApi",
        "com.google.android.gms.fitness.GoalsApi",
        "com.google.android.gms.fitness.GoogleFitnessService.START",
        "com.google.android.gms.fitness.HistoryApi",
        "com.google.android.gms.fitness.InternalApi",
        "com.google.android.gms.fitness.RecordingApi",
        "com.google.android.gms.fitness.SensorsApi",
        "com.google.android.gms.fitness.SessionsApi",
        "com.google.android.gms.fonts.service.START",
        "com.google.android.gms.freighter.service.START",
        "com.google.android.gms.games.internal.connect.service.START",
        "com.google.android.gms.games.PLAY_GAMES_UPGRADE",
        "com.google.android.gms.games.service.START",
        "com.google.android.gms.gass.START",
        "com.google.android.gms.gmscompliance.service.START",
        "com.google.android.gms.googlehelp.HELP",
        "com.google.android.gms.googlehelp.service.GoogleHelpService.START",
        "com.google.android.gms.growth.service.START",
        "com.google.android.gms.herrevad.services.LightweightNetworkQualityAndroidService.START",
        "com.google.android.gms.icing.INDEX_SERVICE",
        "com.google.android.gms.icing.LIGHTWEIGHT_INDEX_SERVICE",
        "com.google.android.gms.identity.service.BIND",
        "com.google.android.gms.inappreach.service.START",
        "com.google.android.gms.instantapps.START",
        "com.google.android.gms.kids.service.START",
        "com.google.android.gms.languageprofile.service.START",
        "com.google.android.gms.learning.internal.dynamitesupport.START",
        "com.google.android.gms.learning.intservice.START",
        "com.google.android.gms.learning.predictor.START",
        "com.google.android.gms.learning.trainer.START",
        "com.google.android.gms.learning.training.background.START",
        "com.google.android.gms.location.places.GeoDataApi",
        "com.google.android.gms.location.places.PlaceDetectionApi",
        "com.google.android.gms.location.places.PlacesApi",
        "com.google.android.gms.location.reporting.service.START",
        "com.google.android.gms.location.settings.LOCATION_HISTORY",
        "com.google.android.gms.location.settings.LOCATION_REPORTING_SETTINGS",
        "com.google.android.location.internal.GoogleLocationManagerService",
        "com.google.android.gms.locationsharing.api.START",
        "com.google.android.gms.locationsharingreporter.service.START",
        "com.google.android.gms.lockbox.service.START",
        "com.google.android.gms.matchstick.lighter.service.START",
        "com.google.android.gms.mdm.services.DeviceManagerApiService.START",
        "com.google.android.gms.mdm.services.START",
        "com.google.android.gms.mdns.service.START",
        "com.google.android.gms.measurement.START",
        "com.google.android.gms.nearby.bootstrap.service.NearbyBootstrapService.START",
        "com.google.android.gms.nearby.connection.service.START",
        "com.google.android.gms.nearby.fastpair.START",
        "com.google.android.gms.nearby.messages.service.NearbyMessagesService.START",
        "com.google.android.gms.nearby.sharing.service.NearbySharingService.START",
        "com.google.android.gms.nearby.sharing.START_SERVICE",
        "com.google.android.gms.notifications.service.START",
        "com.google.android.gms.ocr.service.internal.START",
        "com.google.android.gms.ocr.service.START",
        "com.google.android.gms.oss.licenses.service.START",
        "com.google.android.gms.payse.service.BIND",
        "com.google.android.gms.people.contactssync.service.START",
        "com.google.android.gms.people.service.START",
        "com.google.android.gms.phenotype.service.START",
        "com.google.android.gms.photos.autobackup.service.START",
        "com.google.android.gms.playlog.service.START",
        "com.google.android.gms.plus.service.default.INTENT",
        "com.google.android.gms.plus.service.image.INTENT",
        "com.google.android.gms.plus.service.internal.START",
        "com.google.android.gms.plus.service.START",
        "com.google.android.gms.potokens.service.START",
        "com.google.android.gms.pseudonymous.service.START",
        "com.google.android.gms.rcs.START",
        "com.google.android.gms.reminders.service.START",
        "com.google.android.gms.romanesco.MODULE_BACKUP_AGENT",
        "com.google.android.gms.romanesco.service.START",
        "com.google.android.gms.safetynet.service.START",
        "com.google.android.gms.scheduler.ACTION_PROXY_SCHEDULE",
        "com.google.android.gms.search.service.SEARCH_AUTH_START",
        "com.google.android.gms.semanticlocation.service.START_ODLH",
        "com.google.android.gms.sesame.service.BIND",
        "com.google.android.gms.settings.EXPOSURE_NOTIFICATION_SETTINGS",
        "com.google.android.gms.setup.auth.SecondDeviceAuth.START",
        "com.google.android.gms.signin.service.START",
        "com.google.android.gms.smartdevice.d2d.SourceDeviceService.START",
        "com.google.android.gms.smartdevice.d2d.TargetDeviceService.START",
        "com.google.android.gms.smartdevice.directtransfer.SourceDirectTransferService.START",
        "com.google.android.gms.smartdevice.directtransfer.TargetDirectTransferService.START",
        "com.google.android.gms.smartdevice.postsetup.PostSetupService.START",
        "com.google.android.gms.smartdevice.setup.accounts.AccountsService.START",
        "com.google.android.gms.smartdevice.wifi.START_WIFI_HELPER_SERVICE",
        "com.google.android.gms.social.location.activity.service.START",
        "com.google.android.gms.speech.service.START",
        "com.google.android.gms.statementservice.EXECUTE",
        "com.google.android.gms.stats.ACTION_UPLOAD_DROPBOX_ENTRIES",
        "com.google.android.gms.tapandpay.service.BIND",
        "com.google.android.gms.telephonyspam.service.START",
        "com.google.android.gms.testsupport.service.START",
        "com.google.android.gms.thunderbird.service.START",
        "com.google.android.gms.trustagent.BridgeApi.START",
        "com.google.android.gms.trustagent.StateApi.START",
        "com.google.android.gms.trustagent.trustlet.trustletmanagerservice.BIND",
        "com.google.android.gms.trustlet.bluetooth.service.BIND",
        "com.google.android.gms.trustlet.connectionlessble.service.BIND",
        "com.google.android.gms.trustlet.face.service.BIND",
        "com.google.android.gms.trustlet.nfc.service.BIND",
        "com.google.android.gms.trustlet.onbody.service.BIND",
        "com.google.android.gms.trustlet.place.service.BIND",
        "com.google.android.gms.trustlet.voiceunlock.service.BIND",
        "com.google.android.gms.udc.service.START",
        "com.google.android.gms.update.START_API_SERVICE",
        "com.google.android.gms.update.START_SERVICE",
        "com.google.android.gms.update.START_SINGLE_USER_API_SERVICE",
        "com.google.android.gms.update.START_TV_API_SERVICE",
        "com.google.android.gms.usagereporting.service.START",
        "com.google.android.gms.userlocation.service.START",
        "com.google.android.gms.vehicle.cabin.service.START",
        "com.google.android.gms.vehicle.climate.service.START",
        "com.google.android.gms.vehicle.info.service.START",
        "com.google.android.gms.wallet.firstparty.ACTION_PURCHASE_MANAGER",
        "com.google.android.gms.wallet.service.BIND",
        "com.google.android.gms.walletp2p.service.firstparty.BIND",
        "com.google.android.gms.walletp2p.service.zeroparty.BIND",
        "com.google.android.gms.wearable.BIND",
        "com.google.android.gms.wearable.BIND_LISTENER",
        "com.google.android.gms.wearable.DATA_CHANGED",
        "com.google.android.gms.wearable.MESSAGE_RECEIVED",
        "com.google.android.gms.wearable.NODE_CHANGED",
        "com.google.android.gsf.action.GET_GLS",
        "com.google.android.location.settings.LOCATION_REPORTING_SETTINGS",
        "com.google.android.mdd.service.START",
        "com.google.android.mdh.service.listener.START",
        "com.google.android.mdh.service.START",
        "com.google.android.mobstore.service.START",
        "com.google.iid.TOKEN_REQUEST",
        "com.google.android.gms.location.places.ui.PICK_PLACE",
    )

    val AUTHORITIES = setOf(
        "com.google.android.gms.auth.accounts",
        "com.google.android.gms.chimera",
        "com.google.android.gms.fonts",
        "com.google.android.gms.phenotype",
        "com.google.android.gsf.gservices",
        "com.google.android.gsf.subscribedfeeds",
        "com.google.settings",
    )
}

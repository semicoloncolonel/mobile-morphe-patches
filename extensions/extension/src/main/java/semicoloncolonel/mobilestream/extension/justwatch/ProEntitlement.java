package semicoloncolonel.mobilestream.extension.justwatch;

import com.revenuecat.purchases.EntitlementInfo;
import com.revenuecat.purchases.OwnershipType;
import com.revenuecat.purchases.PeriodType;
import com.revenuecat.purchases.Store;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONObject;

public final class ProEntitlement {

    private static final String[] IDS = {
        "pro", "premium", "justwatch_pro", "jw_pro",
        "pro_access", "plus", "justwatch_plus", "jw_plus"
    };

    private ProEntitlement() {}

    public static Map<String, EntitlementInfo> patchActive(Map<String, EntitlementInfo> real) {
        return patch(real);
    }

    public static Map<String, EntitlementInfo> patchAll(Map<String, EntitlementInfo> real) {
        return patch(real);
    }

    private static Map<String, EntitlementInfo> patch(Map<String, EntitlementInfo> real) {
        Map<String, EntitlementInfo> out = new LinkedHashMap<>();
        if (real != null) {
            out.putAll(real);
        }
        for (String id : IDS) {
            out.put(id, fakeEntitlement(id));
        }
        return out;
    }

    private static EntitlementInfo fakeEntitlement(String id) {
        // Minimal fake EntitlementInfo that reports as active/pro.
        // Adjust constructor args if your RevenueCat version differs.
        JSONObject json = new JSONObject();
        try {
            json.put("identifier", id);
            json.put("isActive", true);
            json.put("willRenew", true);
            json.put("periodType", "NORMAL");
            json.put("latestPurchaseDate", new Date().getTime());
            json.put("originalPurchaseDate", new Date().getTime());
            json.put("expirationDate", (Object) null);
            json.put("store", "PLAY_STORE");
            json.put("ownershipType", "PURCHASED");
        } catch (Exception ignored) {}

        return new EntitlementInfo(
            id,
            true,               // isActive
            true,               // willRenew
            PeriodType.NORMAL,
            new Date(),         // latestPurchaseDate
            new Date(),         // originalPurchaseDate
            null,               // expirationDate
            Store.PLAY_STORE,
            OwnershipType.PURCHASED,
            json
        );
    }
}

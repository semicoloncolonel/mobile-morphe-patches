package semicoloncolonel.mobilestream.extension.justwatch;

import java.lang.reflect.Constructor;
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

    @SuppressWarnings("unchecked")
    public static Map<String, Object> patchActive(Map<String, Object> real) {
        return patch(real);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> patchAll(Map<String, Object> real) {
        return patch(real);
    }

    private static Map<String, Object> patch(Map<String, Object> real) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (real != null) {
            out.putAll(real);
        }
        for (String id : IDS) {
            Object fake = fakeEntitlement(id);
            if (fake != null) {
                out.put(id, fake);
            }
        }
        return out;
    }

    private static Object fakeEntitlement(String id) {
        try {
            Class<?> entitlementClass = Class.forName("com.revenuecat.purchases.EntitlementInfo");
            Class<?> periodTypeClass = Class.forName("com.revenuecat.purchases.PeriodType");
            Class<?> storeClass = Class.forName("com.revenuecat.purchases.Store");
            Class<?> ownershipClass = Class.forName("com.revenuecat.purchases.OwnershipType");

            Object periodNormal = Enum.valueOf((Class<Enum>) periodTypeClass, "NORMAL");
            Object storePlay = Enum.valueOf((Class<Enum>) storeClass, "PLAY_STORE");
            Object ownershipPurchased = Enum.valueOf((Class<Enum>) ownershipClass, "PURCHASED");

            JSONObject json = new JSONObject();
            json.put("identifier", id);
            json.put("isActive", true);
            json.put("willRenew", true);
            json.put("periodType", "NORMAL");
            json.put("latestPurchaseDate", System.currentTimeMillis());
            json.put("originalPurchaseDate", System.currentTimeMillis());
            json.put("expirationDate", JSONObject.NULL);
            json.put("store", "PLAY_STORE");
            json.put("productIdentifier", id);
            json.put("ownershipType", "PURCHASED");

            // Try the modern public constructor (15 args, no verification)
            Constructor<?> ctor = findConstructor(entitlementClass, 15);
            if (ctor != null) {
                return ctor.newInstance(
                    id, true, true,
                    periodNormal,
                    new Date(), new Date(), null,
                    storePlay,
                    id, null, false,
                    null, null,
                    ownershipPurchased,
                    json
                );
            }

            // Fallback: older shorter constructor if present
            ctor = findConstructor(entitlementClass, 10);
            if (ctor != null) {
                return ctor.newInstance(
                    id, true, true,
                    periodNormal,
                    new Date(), new Date(), null,
                    storePlay,
                    ownershipPurchased,
                    json
                );
            }

            return null;
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    private static Constructor<?> findConstructor(Class<?> clazz, int paramCount) {
        for (Constructor<?> c : clazz.getConstructors()) {
            if (c.getParameterCount() == paramCount) {
                return c;
            }
        }
        return null;
    }
}

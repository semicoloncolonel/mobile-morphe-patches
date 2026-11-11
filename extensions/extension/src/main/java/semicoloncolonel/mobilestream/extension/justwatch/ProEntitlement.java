cat > extensions/justwatch/src/main/java/semicoloncolonel/mobilestream/extension/justwatch/ProEntitlement.java << 'EOF'
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
        Map<String, EntitlementInfo> out = new LinkedHashMap<>(real != null ? real : Map.of());
        for (String id : IDS) {
            if (!out.containsKey(id)) {
                out.put(id, build(id));
            }
        }
        return out;
    }

    public static Map<String, EntitlementInfo> patchAll(Map<String, EntitlementInfo> real) {
        return patchActive(real);
    }

    private static EntitlementInfo build(String id) {
        Date now = new Date();
        Date farFuture = new Date(now.getTime() + 100L * 365 * 24 * 60 * 60 * 1000);

        return new EntitlementInfo(
            id,
            true,
            true,
            PeriodType.NORMAL,
            now,
            now,
            farFuture,
            Store.PLAY_STORE,
            id,
            null,
            false,
            null,
            null,
            OwnershipType.PURCHASED,
            new JSONObject()
        );
    }
}
EOF
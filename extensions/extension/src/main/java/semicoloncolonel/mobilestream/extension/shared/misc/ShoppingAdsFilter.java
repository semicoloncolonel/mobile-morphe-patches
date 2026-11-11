package semicoloncolonel.mobilestream.extension.shared.misc;

import android.util.Log;

/**
 * "Hide sponsored listings" for the shopping apps.
 *
 * Asks the store's own services for results without sponsored products, using the switches
 * those services already have. Everything is decided on-device; nothing is sent anywhere by
 * this class.
 */
@SuppressWarnings("unused")
public final class ShoppingAdsFilter {
    private static final String TAG = "MSP-ADS";

    private static boolean logged;

    private ShoppingAdsFilter() {
    }

    /**
     * Amazon draws search results and product pages as web pages. Sponsored products there are
     * marked by Amazon's own markup: the ad holder and sponsored-label classes in search
     * results, the sponsored-products and ad-placement sections on product pages, and the
     * ad frames used for sponsored brands and video.
     */
    private static final String[] AMAZON_SPONSORED = {
            // Search results
            ".s-result-item.AdHolder",
            ".s-result-item:has(.puis-sponsored-label-text)",
            ".s-result-item:has([data-component-type=\"sp-sponsored-result\"])",
            ".s-result-item:has(.sbv-ad-content-container)",
            ".s-result-item:has([data-ad-feedback])",
            ".sbv-video-single-product",
            // Product pages
            "#sponsoredProducts_feature_div",
            "#sponsoredProducts2_feature_div",
            "[id^=\"sp_detail\"]",
            ".sponsored-products-detail-mobile",
            "[cel_widget_id*=\"sp_atf\"]",
            "[cel_widget_id*=\"sp_btf\"]",
            "[cel_widget_id*=\"adplacements\"]",
            // Carousels and ad frames anywhere
            ".a-carousel-card:has(.p13n-sc-sponsored-label)",
            ".a-carousel-card:has([data-ad-placement-metadata])",
            "[data-ad-id]",
            ".amzn-safe-frame-container",
            "[id^=\"ape_\"]",
    };

    private static final String HIDE_AMAZON_SPONSORED = "(function () {\n"
            + "  if (document.getElementById('msp-hide-sponsored')) { return; }\n"
            + "  var style = document.createElement('style');\n"
            + "  style.id = 'msp-hide-sponsored';\n"
            + "  style.textContent = " + jsString(android.text.TextUtils.join(",", AMAZON_SPONSORED)) + "\n"
            + "    + '{display:none !important}';\n"
            + "  (document.head || document.documentElement).appendChild(style);\n"
            + "})();";

    private static String jsString(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    /** Amazon: hides sponsored products on the page the app's web view is showing. */
    public static void hideAmazonSponsored(android.webkit.WebView webView) {
        try {
            if (webView != null) {
                webView.evaluateJavascript(HIDE_AMAZON_SPONSORED, null);
            }
        } catch (Throwable t) {
            Log.w(TAG, "hide Amazon sponsored products failed: " + t);
        }
    }

    /**
     * Target: called with each search parameter's name and value as the request is built.
     * include_sponsored is answered with false; every other parameter keeps its value.
     */
    public static String targetSearchParameter(String name, String value) {
        if (!"include_sponsored".equals(name)) {
            return value;
        }
        if (!logged) {
            logged = true;
            Log.i(TAG, "search without sponsored products");
        }
        return "false";
    }

    /**
     * Lowe's: called with each query parameter's name and value as Retrofit builds a request.
     * enableSponsoredSearch is answered with false; every other parameter keeps its value.
     */
    public static String lowesQueryParameter(String name, String value) {
        if (!"enableSponsoredSearch".equals(name)) {
            return value;
        }
        if (!logged) {
            logged = true;
            Log.i(TAG, "search without sponsored products");
        }
        return "false";
    }

    /**
     * Lowe's: the feature switches for the sponsored products row on product pages, the ad
     * banner on collection pages, and the partner offers after checkout.
     */
    public static boolean isLowesAdSwitch(Object featureSwitch) {
        if (!(featureSwitch instanceof Enum)) {
            return false;
        }
        String name = ((Enum<?>) featureSwitch).name();
        return "ENABLE_SPONSORED_PRODUCTS_ON_PDP".equals(name)
                || "ENABLE_GAM_BANNER_ON_CDP".equals(name)
                || "ENABLE_ROKT_ADS".equals(name);
    }

    /**
     * Walgreens: the feature flags for the Criteo sponsored carousels and display banners
     * (criteo...), the Google Ad Manager banners and promo cards (gam...), and the Rokt offers
     * after checkout (ROKT). The flag's class is renamed in every release, so its key is read
     * through getKey() by reflection. A flag whose key cannot be read is left alone.
     */
    public static boolean isWalgreensAdFlag(Object flag) {
        if (flag == null) {
            return false;
        }
        try {
            Object key = flag.getClass().getMethod("getKey").invoke(flag);
            if (!(key instanceof String)) {
                return false;
            }
            String k = (String) key;
            return k.startsWith("criteo") || k.startsWith("gam") || k.equals("ROKT");
        } catch (Throwable t) {
            return false;
        }
    }

    // ── Walmart ─────────────────────────────────────────────────────────────

    private static final String[] WALMART_AD_SUFFIXES = {
            ".reqenable", ".reqenabled", ".req.enabled",
            ".renderenable", ".renderenabled", ".render.enabled",
    };

    private static final java.util.Set<String> WALMART_AD_KEYS = new java.util.HashSet<>(java.util.Arrays.asList(
            // With this on, one server model decides every ad surface and the flags below are
            // not read.
            "ads.primaryCCM.enabled",
            "ads.ip.sponsoredPrompt.enabled",
            "ads.deals.mosaic.enabled",
            "ads.gam.enabled",
            "ads.ty.ob.fluent.enabled",
            "ads.checkin.videoCard.enable",
            "ads.miniGallery.enabled",
            "ads.ip.galTop.enabled",
            "ads.ip.galMiddle.enabled",
            "ads.ip.galBottom.enabled",
            "ads.supportPostTxnSkylineAds.enabled",
            "ads.sp.sv.prompt.enabled",
            "ads.ip.bb.spVideo.enabled",
            "homepage.sponsored.carousel.enabled",
            "contentPages.sponsored.carousel.enabled",
            "checkout.thankYou.ads.enabled",
            "feature.search.tileTakeOverAd.enabled",
            "feature.browse.tileTakeOverAd.enabled",
            "feature.shop.tileTakeOverAd.enabled"));

    /**
     * Walmart: the on/off flags of its ad surfaces. Each surface has a "request" and a
     * "render" flag in the ads. namespace (ads.sp.spc.reqEnable, ads.ip.sba.render.enabled, and
     * so on), and a few surfaces have a flag of their own. Flags that tune layout or tracking,
     * and the flags where true hides an ad, are left alone.
     */
    public static boolean isWalmartAdKey(String key) {
        if (key == null) {
            return false;
        }
        if (WALMART_AD_KEYS.contains(key)) {
            return true;
        }
        if (!key.startsWith("ads.")) {
            return false;
        }
        String k = key.toLowerCase(java.util.Locale.US);
        for (String suffix : WALMART_AD_SUFFIXES) {
            if (k.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private static final java.util.Map<String, java.lang.reflect.Method> getters =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** Calls a no-argument getter by name, or answers null when the object has none. */
    private static Object get(Object target, String getter) {
        if (target == null) {
            return null;
        }
        try {
            String id = target.getClass().getName() + '#' + getter;
            java.lang.reflect.Method method = getters.get(id);
            if (method == null) {
                method = target.getClass().getMethod(getter);
                getters.put(id, method);
            }
            return method.invoke(target);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Walmart: called with the products of each search or category result as the response is
     * read. A product is sponsored when its item or product tile carries sponsoredProduct data,
     * which is the test the app itself uses. Those are left out of the list that is returned.
     * When anything is unexpected, the list is returned as it came.
     */
    public static java.util.List<?> walmartSearchItems(java.util.List<?> items) {
        if (items == null || items.isEmpty()) {
            return items;
        }
        try {
            java.util.ArrayList<Object> kept = new java.util.ArrayList<>(items.size());
            for (Object item : items) {
                boolean sponsored = get(get(item, "getItemFragment"), "getSponsoredProduct") != null
                        || get(get(item, "getProductTileSharedFragment"), "getSponsoredProduct") != null;
                if (!sponsored) {
                    kept.add(item);
                }
            }
            if (kept.size() == items.size()) {
                return items;
            }
            if (!logged) {
                logged = true;
                Log.i(TAG, "left out " + (items.size() - kept.size()) + " sponsored products");
            }
            return kept;
        } catch (Throwable t) {
            return items;
        }
    }

    // ── The Home Depot ──────────────────────────────────────────────────────

    /**
     * The Home Depot: called with the children of each product list on the newer search and
     * category pages. A child that is a ProductItem holds a ProductModel, whose only boolean
     * field says whether the product is sponsored. The member names are renamed in every
     * release, so they are found by type. Sponsored products are left out of the list that is
     * returned. When anything is unexpected, the list is returned as it came.
     */
    public static java.util.List<?> homeDepotProductList(java.util.List<?> children) {
        if (children == null || children.isEmpty()) {
            return children;
        }
        try {
            java.util.ArrayList<Object> kept = new java.util.ArrayList<>(children.size());
            for (Object child : children) {
                if (!isHomeDepotSponsored(child)) {
                    kept.add(child);
                }
            }
            return kept.size() == children.size() ? children : kept;
        } catch (Throwable t) {
            return children;
        }
    }

    private static boolean isHomeDepotSponsored(Object child) throws IllegalAccessException {
        if (child == null || !child.getClass().getName().endsWith("SDUIComponent$ProductItem")) {
            return false;
        }
        for (java.lang.reflect.Field field : child.getClass().getDeclaredFields()) {
            if (!field.getType().getName().endsWith(".ProductModel")) {
                continue;
            }
            field.setAccessible(true);
            Object model = field.get(child);
            if (model == null) {
                return false;
            }
            java.lang.reflect.Field flag = null;
            for (java.lang.reflect.Field candidate : model.getClass().getDeclaredFields()) {
                if (candidate.getType() == boolean.class
                        && !java.lang.reflect.Modifier.isStatic(candidate.getModifiers())) {
                    if (flag != null) {
                        return false; // More than one boolean: not the model this was written for.
                    }
                    flag = candidate;
                }
            }
            if (flag == null) {
                return false;
            }
            flag.setAccessible(true);
            return flag.getBoolean(model);
        }
        return false;
    }

    // ── CVS ─────────────────────────────────────────────────────────────────

    /** CVS: the flag for the sponsored suggestion at the top of search suggestions. */
    public static boolean isCvsAdFlag(Object flag) {
        return "unified-sponsored-search-results".equals(get(flag, "getKey"));
    }

    private static final String[] CVS_SPONSORED_SYSTEMS = {
            "EXCLUDE_CRITEO_INGRID", "EXCLUDE_CREALYTICS", "EXCLUDE_CRITEO_BUTTERFLY",
    };

    /**
     * CVS: called with the list of systems a search request asks the server to leave out. The
     * sponsored product systems are added, which is what the app's own Buy It Again and Deals
     * searches send. A request without a list gets the app's default as well, since the app
     * only fills that in for a missing list. When anything is unexpected, the list is returned
     * as it came.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static java.util.List<?> cvsSearchExclusions(java.util.List<?> exclusions) {
        try {
            Class<? extends Enum> type = Class
                    .forName("com.cvs.shop.network.productSearch.api.models.SystemExclusions")
                    .asSubclass(Enum.class);
            java.util.ArrayList<Object> all = new java.util.ArrayList<>();
            if (exclusions == null) {
                all.add(Enum.valueOf(type, "EXCLUDE_STORE_SERVICE"));
            } else {
                all.addAll(exclusions);
            }
            for (String name : CVS_SPONSORED_SYSTEMS) {
                Object system = Enum.valueOf(type, name);
                if (!all.contains(system)) {
                    all.add(system);
                }
            }
            return all;
        } catch (Throwable t) {
            return exclusions;
        }
    }
}

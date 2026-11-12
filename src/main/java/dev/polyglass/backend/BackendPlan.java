package dev.polyglass.backend;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Ordering policy for the backend candidate list.
 *
 * <p>Deliberately free of Minecraft, Fabric and I/O imports so it can be unit
 * tested on a bare JVM. {@link BackendCatalog} is the only caller that knows
 * about {@code GpuBackend} instances.</p>
 *
 * <p>The result is a preference order, not a guarantee: Minecraft tries each
 * candidate in turn and falls through to the next one when
 * {@code createDevice} throws {@code BackendCreationException}. That means a
 * broken or unfinished backend degrades to the next entry instead of crashing,
 * which is exactly the behaviour a fallback chain needs.</p>
 */
public final class BackendPlan {

    /** Sentinel meaning "no explicit choice, follow {@code order}". */
    public static final String AUTO = "auto";

    private BackendPlan() {
    }

    /**
     * @param requested explicit backend id, or {@code auto}
     * @param order     user-configured preference order
     * @param available ids that can actually be constructed in this session
     * @return ids to try, most preferred first; empty only if nothing is available
     */
    public static List<String> resolve(String requested, List<String> order, List<String> available) {
        Set<String> usable = new LinkedHashSet<>(available);
        List<String> plan = new ArrayList<>();

        if (requested != null) {
            String id = normalize(requested);
            if (!AUTO.equals(id) && usable.contains(id)) {
                plan.add(id);
            }
        }

        if (order != null) {
            for (String entry : order) {
                String id = normalize(entry);
                if (usable.contains(id) && !plan.contains(id)) {
                    plan.add(id);
                }
            }
        }

        // Anything available but never mentioned is still worth trying, after
        // the explicit choices, so a new backend is never dead on arrival.
        for (String id : available) {
            if (usable.contains(id) && !plan.contains(id)) {
                plan.add(id);
            }
        }

        return plan;
    }

    public static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isAuto(String id) {
        return AUTO.equals(normalize(id));
    }
}

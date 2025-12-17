package io.reflectoring.carshippingbackend.services;

import io.reflectoring.carshippingbackend.tables.Motorcycle;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MotorcycleSpecification {

    public static Specification<Motorcycle> byFilters(Map<String, String> params) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();

            // Helper for case-insensitive comparison
            java.util.function.Function<String, String> safeLower =
                    (v) -> v == null ? "" : v.toLowerCase();

            // ============= TEXT FILTERS =============
            if (params.containsKey("brand") && !params.get("brand").isBlank()) {
                preds.add(cb.like(cb.lower(root.get("brand")),
                        "%" + safeLower.apply(params.get("brand")) + "%"));
            }

            if (params.containsKey("model") && !params.get("model").isBlank()) {
                preds.add(cb.like(cb.lower(root.get("model")),
                        "%" + safeLower.apply(params.get("model")) + "%"));
            }

            if (params.containsKey("type") && !params.get("type").isBlank()) {
                preds.add(cb.equal(cb.lower(root.get("type")),
                        safeLower.apply(params.get("type"))));
            }

            if (params.containsKey("location") && !params.get("location").isBlank()) {
                preds.add(cb.like(cb.lower(root.get("location")),
                        "%" + safeLower.apply(params.get("location")) + "%"));
            }

            if (params.containsKey("owner") && !params.get("owner").isBlank()) {
                preds.add(cb.equal(cb.lower(root.get("owner")),
                        safeLower.apply(params.get("owner"))));
            }

            if (params.containsKey("status") && !params.get("status").isBlank()) {
                preds.add(cb.equal(cb.lower(root.get("status")),
                        safeLower.apply(params.get("status"))));
            }

            // ============= NUMERIC RANGE FILTERS =============
            // Price filters
            if (params.containsKey("minPrice")) {
                try {
                    double minPrice = Double.parseDouble(params.get("minPrice"));
                    preds.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
                } catch (NumberFormatException ignored) {}
            }

            if (params.containsKey("maxPrice")) {
                try {
                    double maxPrice = Double.parseDouble(params.get("maxPrice"));
                    preds.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
                } catch (NumberFormatException ignored) {}
            }

            // Year filters
            if (params.containsKey("minYear")) {
                try {
                    int minYear = Integer.parseInt(params.get("minYear"));
                    preds.add(cb.greaterThanOrEqualTo(root.get("year"), minYear));
                } catch (NumberFormatException ignored) {}
            }

            if (params.containsKey("maxYear")) {
                try {
                    int maxYear = Integer.parseInt(params.get("maxYear"));
                    preds.add(cb.lessThanOrEqualTo(root.get("year"), maxYear));
                } catch (NumberFormatException ignored) {}
            }

            // Engine capacity filters
            if (params.containsKey("minEngineCapacity")) {
                try {
                    int minCC = Integer.parseInt(params.get("minEngineCapacity"));
                    preds.add(cb.greaterThanOrEqualTo(root.get("engineCapacity"), minCC));
                } catch (NumberFormatException ignored) {}
            }

            if (params.containsKey("maxEngineCapacity")) {
                try {
                    int maxCC = Integer.parseInt(params.get("maxEngineCapacity"));
                    preds.add(cb.lessThanOrEqualTo(root.get("engineCapacity"), maxCC));
                } catch (NumberFormatException ignored) {}
            }

            // ============= FULL-TEXT SEARCH =============
            if (params.containsKey("search") && !params.get("search").isBlank()) {
                String keyword = "%" + safeLower.apply(params.get("search")) + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("brand")), keyword),
                        cb.like(cb.lower(root.get("model")), keyword),
                        cb.like(cb.lower(root.get("type")), keyword),
                        cb.like(cb.lower(root.get("location")), keyword),
                        cb.like(cb.lower(root.get("description")), keyword)
                ));
            }

            // ============= PRICE RANGE STRING (e.g., "100000-500000") =============
            if (params.containsKey("priceRange") && !params.get("priceRange").isBlank()) {
                try {
                    String[] range = params.get("priceRange").split("-");
                    if (range.length == 2) {
                        double min = Double.parseDouble(range[0].trim());
                        double max = Double.parseDouble(range[1].trim());
                        preds.add(cb.between(root.get("price"), min, max));
                    }
                } catch (NumberFormatException ignored) {}
            }

            // Combine all predicates
            return cb.and(preds.toArray(new Predicate[0]));
        };
    }

    // ============= ROLE-BASED SPECIFICATIONS =============

    public static Specification<Motorcycle> byOwner(String ownerEmail) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("owner")), ownerEmail.toLowerCase());
    }

    public static Specification<Motorcycle> byStatus(String status) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("status")), status.toLowerCase());
    }

    public static Specification<Motorcycle> byApprovedStatus() {
        return byStatus("approved");
    }
}
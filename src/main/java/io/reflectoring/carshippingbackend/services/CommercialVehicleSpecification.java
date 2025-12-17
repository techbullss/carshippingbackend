package io.reflectoring.carshippingbackend.services;

import io.reflectoring.carshippingbackend.tables.CommercialVehicle;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommercialVehicleSpecification {

    public static Specification<CommercialVehicle> byFilters(Map<String, String> params) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();

            // Helper to lowercase comparisons safely
            java.util.function.Function<String, String> safeLower =
                    (v) -> v == null ? "" : v.toLowerCase();

            // ============= BASIC TEXT FILTERS =============
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

            if (params.containsKey("fuelType") && !params.get("fuelType").isBlank()) {
                preds.add(cb.equal(cb.lower(root.get("fuelType")),
                        safeLower.apply(params.get("fuelType"))));
            }

            if (params.containsKey("bodyType") && !params.get("bodyType").isBlank()) {
                preds.add(cb.equal(cb.lower(root.get("bodyType")),
                        safeLower.apply(params.get("bodyType"))));
            }

            if (params.containsKey("conditionType") && !params.get("conditionType").isBlank()) {
                preds.add(cb.equal(cb.lower(root.get("conditionType")),
                        safeLower.apply(params.get("conditionType"))));
            }

            if (params.containsKey("color") && !params.get("color").isBlank()) {
                preds.add(cb.equal(cb.lower(root.get("color")),
                        safeLower.apply(params.get("color"))));
            }

            if (params.containsKey("engineType") && !params.get("engineType").isBlank()) {
                preds.add(cb.equal(cb.lower(root.get("engineType")),
                        safeLower.apply(params.get("engineType"))));
            }

            if (params.containsKey("transmission") && !params.get("transmission").isBlank()) {
                preds.add(cb.equal(cb.lower(root.get("transmission")),
                        safeLower.apply(params.get("transmission"))));
            }

            if (params.containsKey("location") && !params.get("location").isBlank()) {
                preds.add(cb.like(cb.lower(root.get("location")),
                        "%" + safeLower.apply(params.get("location")) + "%"));
            }

            if (params.containsKey("ownerType") && !params.get("ownerType").isBlank()) {
                preds.add(cb.equal(cb.lower(root.get("ownerType")),
                        safeLower.apply(params.get("ownerType"))));
            }

            if (params.containsKey("seller") && !params.get("seller").isBlank()) {
                preds.add(cb.equal(cb.lower(root.get("seller")),
                        safeLower.apply(params.get("seller"))));
            }

            if (params.containsKey("status") && !params.get("status").isBlank()) {
                preds.add(cb.equal(cb.lower(root.get("status")),
                        safeLower.apply(params.get("status"))));
            }

            // ============= RANGE FILTERS =============
            // Price filters (priceKes is Double)
            if (params.containsKey("minPrice")) {
                try {
                    double minPrice = Double.parseDouble(params.get("minPrice"));
                    preds.add(cb.greaterThanOrEqualTo(root.get("priceKes"), minPrice));
                } catch (NumberFormatException ignored) {}
            }

            if (params.containsKey("maxPrice")) {
                try {
                    double maxPrice = Double.parseDouble(params.get("maxPrice"));
                    preds.add(cb.lessThanOrEqualTo(root.get("priceKes"), maxPrice));
                } catch (NumberFormatException ignored) {}
            }

            // Year filters (yearOfManufacture is String)
            if (params.containsKey("minYear")) {
                try {
                    int minYear = Integer.parseInt(params.get("minYear"));
                    preds.add(cb.greaterThanOrEqualTo(
                            cb.toInteger(root.get("yearOfManufacture")), minYear));
                } catch (NumberFormatException ignored) {}
            }

            if (params.containsKey("maxYear")) {
                try {
                    int maxYear = Integer.parseInt(params.get("maxYear"));
                    preds.add(cb.lessThanOrEqualTo(
                            cb.toInteger(root.get("yearOfManufacture")), maxYear));
                } catch (NumberFormatException ignored) {}
            }

            // Mileage filters (mileageKm is String)
            if (params.containsKey("minMileage")) {
                try {
                    int minMileage = Integer.parseInt(params.get("minMileage"));
                    preds.add(cb.greaterThanOrEqualTo(
                            cb.toInteger(root.get("mileageKm")), minMileage));
                } catch (NumberFormatException ignored) {}
            }

            if (params.containsKey("maxMileage")) {
                try {
                    int maxMileage = Integer.parseInt(params.get("maxMileage"));
                    preds.add(cb.lessThanOrEqualTo(
                            cb.toInteger(root.get("mileageKm")), maxMileage));
                } catch (NumberFormatException ignored) {}
            }

            // Engine capacity filters (engineCapacityCc is String)
            if (params.containsKey("minEngineCapacity")) {
                try {
                    int minCC = Integer.parseInt(params.get("minEngineCapacity"));
                    preds.add(cb.greaterThanOrEqualTo(
                            cb.toInteger(root.get("engineCapacityCc")), minCC));
                } catch (NumberFormatException ignored) {}
            }

            if (params.containsKey("maxEngineCapacity")) {
                try {
                    int maxCC = Integer.parseInt(params.get("maxEngineCapacity"));
                    preds.add(cb.lessThanOrEqualTo(
                            cb.toInteger(root.get("engineCapacityCc")), maxCC));
                } catch (NumberFormatException ignored) {}
            }

            // Payload capacity filters (payloadCapacityKg is String)
            if (params.containsKey("minPayload")) {
                try {
                    int minPayload = Integer.parseInt(params.get("minPayload"));
                    preds.add(cb.greaterThanOrEqualTo(
                            cb.toInteger(root.get("payloadCapacityKg")), minPayload));
                } catch (NumberFormatException ignored) {}
            }

            if (params.containsKey("maxPayload")) {
                try {
                    int maxPayload = Integer.parseInt(params.get("maxPayload"));
                    preds.add(cb.lessThanOrEqualTo(
                            cb.toInteger(root.get("payloadCapacityKg")), maxPayload));
                } catch (NumberFormatException ignored) {}
            }

            // Exact year match (for dropdowns)
            if (params.containsKey("yearOfManufacture") && !params.get("yearOfManufacture").isBlank()) {
                String exactYear = params.get("yearOfManufacture");
                preds.add(cb.equal(root.get("yearOfManufacture"), exactYear));
            }

            // ============= FULL-TEXT SEARCH =============
            if (params.containsKey("search") && !params.get("search").isBlank()) {
                String keyword = "%" + safeLower.apply(params.get("search")) + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("brand")), keyword),
                        cb.like(cb.lower(root.get("model")), keyword),
                        cb.like(cb.lower(root.get("bodyType")), keyword),
                        cb.like(cb.lower(root.get("color")), keyword),
                        cb.like(cb.lower(root.get("engineType")), keyword),
                        cb.like(cb.lower(root.get("location")), keyword),
                        cb.like(cb.lower(root.get("conditionType")), keyword),
                        cb.like(cb.lower(root.get("fuelType")), keyword),
                        cb.like(cb.lower(root.get("type")), keyword),
                        cb.like(cb.lower(root.get("seller")), keyword),
                        cb.like(cb.lower(root.get("description")), keyword),
                        cb.like(cb.lower(root.get("features")), keyword)
                ));
            }

            // Combine all predicates
            return cb.and(preds.toArray(new Predicate[0]));
        };
    }

    // ============= ADDITIONAL SPECIFICATIONS =============

    public static Specification<CommercialVehicle> bySeller(String sellerEmail) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("seller")), sellerEmail.toLowerCase());
    }

    public static Specification<CommercialVehicle> byStatus(String status) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("status")), status.toLowerCase());
    }

    public static Specification<CommercialVehicle> byApprovedStatus() {
        return byStatus("approved");
    }
}

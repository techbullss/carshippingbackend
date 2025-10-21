package io.reflectoring.carshippingbackend.services;

import io.reflectoring.carshippingbackend.tables.Car;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CarSpecification {

    public static Specification<Car> byFilters(Map<String, String> params) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();

            // Helper to lowercase comparisons safely
            java.util.function.Function<String, String> safeLower = (v) -> v == null ? "" : v.toLowerCase();

            // ============= BASIC TEXT FILTERS =============
            if (params.containsKey("brand") && !params.get("brand").isBlank()) {
                preds.add(cb.like(cb.lower(root.get("brand")), "%" + safeLower.apply(params.get("brand")) + "%"));
            }

            if (params.containsKey("model") && !params.get("model").isBlank()) {
                preds.add(cb.like(cb.lower(root.get("model")), "%" + safeLower.apply(params.get("model")) + "%"));
            }

            if (params.containsKey("fuelType") && !params.get("fuelType").isBlank()) {
                preds.add(cb.like(cb.lower(root.get("fuelType")), "%" + safeLower.apply(params.get("fuelType")) + "%"));
            }

            if (params.containsKey("bodyType") && !params.get("bodyType").isBlank()) {
                preds.add(cb.like(cb.lower(root.get("bodyType")), "%" + safeLower.apply(params.get("bodyType")) + "%"));
            }

            if (params.containsKey("conditionType") && !params.get("conditionType").isBlank()) {
                preds.add(cb.like(cb.lower(root.get("conditionType")), "%" + safeLower.apply(params.get("conditionType")) + "%"));
            }

            if (params.containsKey("color") && !params.get("color").isBlank()) {
                preds.add(cb.like(cb.lower(root.get("color")), "%" + safeLower.apply(params.get("color")) + "%"));
            }

            if (params.containsKey("engineType") && !params.get("engineType").isBlank()) {
                preds.add(cb.like(cb.lower(root.get("engineType")), "%" + safeLower.apply(params.get("engineType")) + "%"));
            }

            if (params.containsKey("transmission") && !params.get("transmission").isBlank()) {
                preds.add(cb.like(cb.lower(root.get("transmission")), "%" + safeLower.apply(params.get("transmission")) + "%"));
            }

            if (params.containsKey("location") && !params.get("location").isBlank()) {
                preds.add(cb.like(cb.lower(root.get("location")), "%" + safeLower.apply(params.get("location")) + "%"));
            }

            if (params.containsKey("ownerType") && !params.get("ownerType").isBlank()) {
                preds.add(cb.like(cb.lower(root.get("ownerType")), "%" + safeLower.apply(params.get("ownerType")) + "%"));
            }

            if (params.containsKey("seller") && !params.get("seller").isBlank()) {
                preds.add(cb.like(cb.lower(root.get("seller")), "%" + safeLower.apply(params.get("seller")) + "%"));
            }

            // ============= RANGE FILTERS (numbers stored as strings) =============
            // safely parse string fields to numbers for numeric comparisons

            if (params.containsKey("price_gte")) {
                try {
                    double minPrice = Double.parseDouble(params.get("price_gte"));
                    preds.add(cb.greaterThanOrEqualTo(cb.toDouble(root.get("priceKes")), minPrice));
                } catch (NumberFormatException ignored) {}
            }

            if (params.containsKey("price_lte")) {
                try {
                    double maxPrice = Double.parseDouble(params.get("price_lte"));
                    preds.add(cb.lessThanOrEqualTo(cb.toDouble(root.get("priceKes")), maxPrice));
                } catch (NumberFormatException ignored) {}
            }

            if (params.containsKey("year_gte")) {
                try {
                    int minYear = Integer.parseInt(params.get("year_gte"));
                    preds.add(cb.greaterThanOrEqualTo(cb.toInteger(root.get("yearOfManufacture")), minYear));
                } catch (NumberFormatException ignored) {}
            }

            if (params.containsKey("year_lte")) {
                try {
                    int maxYear = Integer.parseInt(params.get("year_lte"));
                    preds.add(cb.lessThanOrEqualTo(cb.toInteger(root.get("yearOfManufacture")), maxYear));
                } catch (NumberFormatException ignored) {}
            }

            if (params.containsKey("mileage_gte")) {
                try {
                    int minMileage = Integer.parseInt(params.get("mileage_gte"));
                    preds.add(cb.greaterThanOrEqualTo(cb.toInteger(root.get("mileageKm")), minMileage));
                } catch (NumberFormatException ignored) {}
            }

            if (params.containsKey("mileage_lte")) {
                try {
                    int maxMileage = Integer.parseInt(params.get("mileage_lte"));
                    preds.add(cb.lessThanOrEqualTo(cb.toInteger(root.get("mileageKm")), maxMileage));
                } catch (NumberFormatException ignored) {}
            }

            if (params.containsKey("engine_cc_gte")) {
                try {
                    int minCC = Integer.parseInt(params.get("engine_cc_gte"));
                    preds.add(cb.greaterThanOrEqualTo(cb.toInteger(root.get("engineCapacityCc")), minCC));
                } catch (NumberFormatException ignored) {}
            }

            if (params.containsKey("engine_cc_lte")) {
                try {
                    int maxCC = Integer.parseInt(params.get("engine_cc_lte"));
                    preds.add(cb.lessThanOrEqualTo(cb.toInteger(root.get("engineCapacityCc")), maxCC));
                } catch (NumberFormatException ignored) {}
            }
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
                        cb.like(cb.lower(root.get("seller")), keyword),
                        cb.like(cb.lower(root.get("description")), keyword)
                ));
            }

            // Combine all predicates
            return cb.and(preds.toArray(new Predicate[0]));
        };
    }
}

package io.reflectoring.carshippingbackend.controllers;

import io.reflectoring.carshippingbackend.Enum.Role;
import io.reflectoring.carshippingbackend.configaration.CustomUserDetails;
import io.reflectoring.carshippingbackend.repository.CarRepository;
import io.reflectoring.carshippingbackend.services.CarService;
import io.reflectoring.carshippingbackend.tables.Car;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@CrossOrigin(origins = "https://f-carshipping.com/") // ADD allowCredentials
@RequestMapping("/api/cars")
public class CarController {

    @Autowired
    private CarRepository carRepo;

    private final CarService service;

    public CarController(CarService service) {
        this.service = service;
    }

    // ------------------- Search / List -------------------
    @GetMapping
    public ResponseEntity<?> search(@RequestParam Map<String,String> allParams,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "12") int size,
                                    @RequestParam(defaultValue = "priceKes,desc") String sort
                                    ) { // ADD Authentication parameter
        try {
            // Check if user is authenticated

            String[] sortParts = sort.split(",");
            Sort s = Sort.by(Sort.Direction.fromString(sortParts.length>1?sortParts[1]:"desc"), sortParts[0]);
            var result = service.search(allParams, page, size, s);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    @PostMapping("/dashboard")
    public ResponseEntity<?> dashboard(
            @RequestParam Map<String, String> allParams,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "priceKes,desc") String sort,
            @RequestBody Map<String, String> userPayload // ✅ Frontend will send this
    ) {
        try {
            String email = userPayload.get("email");
            String role = userPayload.get("role");

            if (email == null || role == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Missing email or role");
            }

            Sort sortObj = Sort.by(Sort.Order.desc("priceKes"));
            Page<Car> cars = service.searchByUserRole(allParams, page, size, sortObj, email, role);

            return ResponseEntity.ok(cars);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch cars: " + e.getMessage());
        }
    }




    // ------------------- Create -------------------
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @RequestPart("car") Car car,
            @RequestPart(value = "images", required = false) MultipartFile[] images,
            Authentication authentication // ADD Authentication parameter

    ) throws IOException {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("Authentication required");
            }

            Car created = service.create(car, images);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }

    }

    // ------------------- Get by ID -------------------
    @GetMapping("/{id}")
    public ResponseEntity<?> getCar(@PathVariable Long id) { // ADD Authentication
        try {

            Optional<Car> car = carRepo.findById(id);
            return ResponseEntity.ok(car.orElse(null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ------------------- Update -------------------
    @PutMapping(value="/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateCar(
            @PathVariable Long id,
            @RequestPart("car") Car car,
            @RequestPart(value = "images", required = false) MultipartFile[] images,
            Authentication authentication // ADD Authentication
    ) throws IOException {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("Authentication required");
            }

            car.setId(id);
            Car updated = service.update(car, images);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ------------------- Similar -------------------
    @GetMapping("/similar")
    public ResponseEntity<?> getSimilarVehicles( // Change return type to ResponseEntity<?>
                                                 @RequestParam String brand,
                                                 @RequestParam String model,
                                                 @RequestParam(required = false) Long exclude
                                                  // ADD Authentication
    ) {
        try {


            List<Car> similarCars = carRepo.findByMakeAndModelAndIdNot(brand, model, exclude);
            return ResponseEntity.ok(similarCars);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ------------------- Latest -------------------
    @GetMapping("/latest")
    public ResponseEntity<?> getLatestArrivals() { // ADD Authentication
        try {


            List<Car> cars = carRepo.findAll(
                    PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "id"))
            ).getContent();
            return ResponseEntity.ok(cars);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ------------------- Delete -------------------
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCar(@PathVariable Long id, Authentication authentication) { // ADD Authentication
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("Authentication required");
            }

            Optional<Car> carOpt = carRepo.findById(id);
            if (carOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            carRepo.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Car deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    // ------------------- Get All Makes -------------------
    @GetMapping("/makes")
    public ResponseEntity<?> getAllMakes() {
        try {
            // DISTINCT make with count of cars per make
            List<Map<String, Object>> makes = carRepo.findDistinctMakesWithCount();
            return ResponseEntity.ok(makes);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ------------------- Get Models by Make -------------------
    @GetMapping("/models")
    public ResponseEntity<?> getModelsByMake(@RequestParam String make) {
        try {
            List<Map<String, Object>> models = carRepo.findDistinctModelsByMake(make);
            return ResponseEntity.ok(models);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

}
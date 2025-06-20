
package com.example.clsc.utils;

import java.util.*;

public class CalculateJsonDiffs {

   // Main method to test deeply nested JSON diff logic
//   public static void main(String[] args) {
//      Map<String, Object> oldJson = new HashMap<>();
//      Map<String, Object> newJson = new HashMap<>();
//
//      // Old JSON structure: user.profile.address.location
//      Map<String, Object> oldLocation = new HashMap<>();
//      oldLocation.put("lat", "28.6139");
//      oldLocation.put("lng", "77.2090");
//
//      Map<String, Object> oldAddress = new HashMap<>();
//      oldAddress.put("city", "Delhi");
//      oldAddress.put("location", oldLocation);
//
//      Map<String, Object> oldProfile = new HashMap<>();
//      oldProfile.put("name", "John Doe");
//      oldProfile.put("age", 30);
//      oldProfile.put("address", oldAddress);
//
//      Map<String, Object> oldUser = new HashMap<>();
//      oldUser.put("profile", oldProfile);
//      oldUser.put("active", true);
//
//      oldJson.put("user", oldUser);
//      oldJson.put("version", 1);
//
//      // New JSON structure with modifications
//      Map<String, Object> newLocation = new HashMap<>();
//      newLocation.put("lat", "28.7041"); // changed
//      newLocation.put("lng", "77.2090");
//
//      Map<String, Object> newAddress = new HashMap<>();
//      newAddress.put("city", "Mumbai"); // changed
//      newAddress.put("location", newLocation);
//
//      Map<String, Object> newProfile = new HashMap<>();
//      newProfile.put("name", "John Doe");
//      newProfile.put("age", 31); // changed
//      newProfile.put("address", newAddress);
//
//      Map<String, Object> newUser = new HashMap<>();
//      newUser.put("profile", newProfile);
//      newUser.put("active", false); // changed
//
//      newJson.put("user", newUser);
//      newJson.put("version", 2); // changed
//      newJson.put("featureEnabled", true); // added
//
//      // Run diff function
//      Map<String, Object[]> diffs = findDiffs(oldJson, newJson);
//
//      // Print the differences
//      System.out.println("DIFFERENCES FOUND:");
//      for (Map.Entry<String, Object[]> entry : diffs.entrySet()) {
//         System.out.printf("%s: [OLD: %s, NEW: %s]%n",
//                 entry.getKey(),
//                 entry.getValue()[0],
//                 entry.getValue()[1]);
//      }
//   }

   // Entry point to start diff checking
   public static Map<String, Object[]> findDiffs(
           Map<String, Object> oldJson,
           Map<String, Object> newJson
   ) {
      Map<String, Object[]> diffs = new LinkedHashMap<>();
      compare(oldJson, newJson, "", diffs);
      return diffs;
   }

   // Recursive compare function for nested maps
   private static void compare(
           Map<String, Object> oldJson,
           Map<String, Object> newJson,
           String path,
           Map<String, Object[]> diffs
   ) {
      for (String key : newJson.keySet()) {
         String fullPath = path.isEmpty() ? key : path + "." + key;
         Object oldValue = oldJson.get(key);
         Object newValue = newJson.get(key);

         if (!oldJson.containsKey(key)) {
            diffs.put(fullPath, new Object[]{null, newValue});
            continue;
         }

         if ((oldValue != null && !oldValue.equals(newValue)) && !(oldValue instanceof Map)) {
            diffs.put(fullPath, new Object[]{oldValue, newValue});
            continue;
         }

         if (oldValue instanceof Map && newValue instanceof Map) {
            compare(
                    (Map<String, Object>) oldValue,
                    (Map<String, Object>) newValue,
                    fullPath,
                    diffs
            );
         }
      }

      for (String key : oldJson.keySet()) {
         if (!newJson.containsKey(key)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            diffs.put(fullPath, new Object[]{oldJson.get(key), null});
         }
      }
   }
}


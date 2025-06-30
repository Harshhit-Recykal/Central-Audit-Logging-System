
package com.example.clsc.utils;

import java.util.*;

public class CalculateJsonDiffs {

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

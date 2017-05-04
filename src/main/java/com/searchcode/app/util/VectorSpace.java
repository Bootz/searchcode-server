/*
 * Copyright (c) 2016 Boyter Online Services
 *
 * Use of this software is governed by the Fair Source License included
 * in the LICENSE.TXT file, but will be eventually open under GNU General Public License Version 3
 * see the README.md for when this clause will take effect
 *
 * Version 1.3.10
 */

package com.searchcode.app.util;

import java.util.HashMap;
import java.util.Map;

public class VectorSpace {

    public double magnitude(Map<String, Integer> concordance) {
        double total = 0;

        for (String key: concordance.keySet()) {
            total += Math.pow(concordance.get(key), 2);
        }

        return Math.sqrt(total);
    }

    public double relation(Map<String, Integer> concordance1, Map<String, Integer> concordance2) {
        double topValue = 0;

        for (String key: concordance1.keySet()) {
            if (concordance2.containsKey(key)) {
                topValue += concordance1.get(key) * concordance2.get(key);
            }
        }

        double mag = this.magnitude(concordance1) * this.magnitude(concordance2);

        if (mag != 0) {
            return topValue / mag;
        }

        return 0;
    }

    public Map<String, Integer> concordance(String text) {
        Map<String, Integer> concordance = new HashMap<>();

        for (String word: text.split(" ")) {
            if (concordance.containsKey(word)) {
                concordance.put(word, concordance.get(word) + 1);
            }
            else {
                concordance.put(word, 1);
            }
        }

        return concordance;
    }
}
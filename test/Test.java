package test;

import java.util.Arrays;
import java.util.HashMap;

public class Test {

    public int findTargetSumWays(int[] nums, int target) {
        if (Arrays.stream(nums).sum() < Math.abs(target)) {
            return 0;
        }
        HashMap<Integer,Integer> map = new HashMap<>();
        map.put(0, 1);
        for (int i = 0; i < nums.length; i++) {
            HashMap<Integer,Integer> temp = new HashMap<>();
            for (int k : map.keySet()) {
                if (temp.get(k+nums[i]) != null) {
                    temp.put(k+nums[i], temp.get(k+nums[i])+map.get(k));
                } else {
                    temp.put(k+nums[i], map.get(k));
                }
            }
            map = temp;
        }
        return map.get(target);
    }

}

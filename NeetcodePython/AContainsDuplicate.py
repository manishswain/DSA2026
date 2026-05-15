#Leetcode Contains Duplicate
#Approach- Use a set to track seen numbers. If we encounter a number that's already in the set, return true. If we finish iterating through the array without finding duplicates, return false.
#Time Complexity: O(n), where n is the length of the input array (since we potentially check each element once).
#Space Complexity: O(n) in the worst case, if all elements are unique and we store them in the set.         
from typing import List


class Solution:
    def containsDuplicate(self, nums: List[int]) -> bool:
        return len(nums) != len(set(nums))
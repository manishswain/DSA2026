# LeetCode 271 - Encode and Decode Strings

Design an algorithm to encode a list of strings into a single string, and a
decode algorithm that can reconstruct the original list of strings from the
encoded string. The strings can contain any characters, including delimiters
like commas or spaces, so a naive join/split will break.

## Brute Force

A naive approach is to join strings with a delimiter (e.g., a comma) and
split on decode. The flaw: if any original string itself contains the
delimiter character, splitting produces the wrong boundaries — the encoding
is ambiguous. This "brute force" is really a broken approach that only works
when you can guarantee the delimiter never appears in the data, which cannot
be guaranteed for arbitrary strings.

- Time: O(n) for encode and decode, where n is total character count.
- Space: O(n) for the encoded/decoded output.

```java
private static String encodeNaive(String[] strs) {
    return String.join(",", strs); // breaks if a string contains ","
}

private static String[] decodeNaive(String s) {
    return s.split(","); // ambiguous / lossy if "," was part of original data
}
```

**Example:** `strs = ["a,b", "c"]`

**Dry Run:**
```
encodeNaive(["a,b", "c"]) -> String.join(",", "a,b", "c") = "a,b,c"

decodeNaive("a,b,c") -> "a,b,c".split(",") = ["a", "b", "c"]
```
Output: `["a", "b", "c"]` — WRONG, original was `["a,b", "c"]`; the comma inside `"a,b"` was mistaken for a delimiter.

## Optimal Solution

Prefix each string with its length followed by a non-numeric delimiter
(here `#`), then concatenate directly: `length#string`. The intuition is
that length-prefixing makes the format self-describing and delimiter-safe —
we never need to search for a "boundary character" inside the string
content, because we already know exactly how many characters to consume
once we've read the length. This works no matter what characters
(including `#` itself) appear inside the original strings.

To decode, scan for the next `#`, parse the number before it as the length
`L`, then read exactly `L` characters after the `#` as the next string, and
continue from there.

- Time: O(n) for encode and O(n) for decode, where n is the total length of
  all strings.
- Space: O(n) for the encoded output / decoded list.

```java
private static String encode(String[] strs) {
    StringBuilder sb = new StringBuilder();
    for (String s : strs) {
        sb.append(s.length()).append('#').append(s);
    }
    return sb.toString();
}

private static String decode(String s) {
    List<String> result = new ArrayList<>();
    int i = 0;
    while (i < s.length()) {
        int j = s.indexOf('#', i);
        int length = Integer.parseInt(s.substring(i, j));
        result.add(s.substring(j + 1, j + 1 + length));
        i = j + 1 + length;
    }
    return result.toString();
}
```

**Example:** `strs = ["a,b", "c"]`

**Dry Run:**
```
encode(["a,b", "c"]):
  "a,b" -> append 3, '#', "a,b" -> sb = "3#a,b"
  "c"   -> append 1, '#', "c"   -> sb = "3#a,b1#c"
encoded = "3#a,b1#c"

decode("3#a,b1#c"):
  i=0: j = indexOf('#', 0) = 1, length = parseInt("3") = 3
       result.add(substring(2, 5)) = "a,b"   -> result = ["a,b"]
       i = 2 + 3 = 5
  i=5: j = indexOf('#', 5) = 6, length = parseInt("1") = 1
       result.add(substring(7, 8)) = "c"     -> result = ["a,b", "c"]
       i = 7 + 1 = 8
  i=8: loop ends (8 == s.length())
```
Output: `["a,b", "c"]` — correctly recovers the original list, including the comma inside `"a,b"`.

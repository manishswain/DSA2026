package Java.Collections;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Senior Java Developer interview revision for Java Collections Framework.
 *
 * Key idea:
 * - List = ordered collection, duplicates allowed.
 * - Set = unique elements, order depends on implementation.
 * - Queue/Deque = FIFO/LIFO/priority based processing.
 * - Map = key-value lookup.
 * - Concurrent collections = thread-safe options for multi-threaded code.
 * - Legacy collections = Vector, Stack, Hashtable. Prefer modern alternatives.
 */
public class AllCollectionRevision {

    public static void main(String[] args) {
        System.out.println("=== Java Collections Revision ===\n");

        listRevision();
        setRevision();
        queueAndDequeRevision();
        mapRevision();
        sortingAndUtilityRevision();
        concurrentCollectionsRevision();
        legacyCollectionsRevision();
        interviewImportantPatterns();
    }

    private static void listRevision() {
        System.out.println("1. LIST REVISION");
        System.out.println("---------------");

        /*
         * ArrayList:
         * - Backed by resizable array.
         * - Fast random access: O(1).
         * - Fast append amortized: O(1).
         * - Insert/remove in middle is O(n).
         * - Most commonly used List implementation.
         */
        List<String> arrayList = new ArrayList<>();
        arrayList.add("Amit");
        arrayList.add("Neha");
        arrayList.add("Ravi");

        arrayList.add(1, "Pooja");
        arrayList.set(0, "Amit Kumar");
        arrayList.remove("Ravi");

        System.out.println("ArrayList: " + arrayList);
        System.out.println("ArrayList get(1): " + arrayList.get(1));
        System.out.println("ArrayList contains Amit: " + arrayList.contains("Amit Kumar"));
        System.out.println("ArrayList indexOf Pooja: " + arrayList.indexOf("Pooja"));
        System.out.println("ArrayList subList(0, 2): " + arrayList.subList(0, 2));

        /*
         * LinkedList:
         * - Doubly linked list.
         * - Fast insert/remove at ends and known iterator position.
         * - Slow random access: O(n).
         * - Also implements Queue and Deque.
         */
        List<String> linkedList = new LinkedList<>();
        linkedList.add("First");
        linkedList.add("Second");
        linkedList.add("Third");

        linkedList.add(1, "Middle");
        linkedList.remove(3);

        System.out.println("LinkedList: " + linkedList);
        System.out.println("LinkedList getFirst if Deque: " + linkedList.getFirst());

        /*
         * Vector:
         * - Legacy synchronized List.
         * - Avoid in new code.
         * - Prefer ArrayList + external synchronization or CopyOnWriteArrayList.
         */
        Vector<String> vector = new Vector<>();
        vector.add("Vector-1");
        vector.add("Vector-2");
        vector.addElement("Vector-3");

        System.out.println("Vector: " + vector);
        System.out.println("Vector capacity: " + vector.capacity());

        /*
         * CopyOnWriteArrayList:
         * - Thread-safe List for many reads and rare writes.
         * - Every mutation copies the underlying array.
         * - Expensive for frequent writes.
         */
        CopyOnWriteArrayList<String> copyOnWriteList = new CopyOnWriteArrayList<>(arrayList);
        copyOnWriteList.addIfAbsent("Neha");
        copyOnWriteList.add("Thread-safe List");

        System.out.println("CopyOnWriteArrayList: " + copyOnWriteList);

        /*
         * List sorting.
         * Comparator is preferred over Comparable when external ordering is needed.
         */
        List<Employee> employees = new ArrayList<>();
        employees.add(new Employee("Amit", 35, 1000));
        employees.add(new Employee("Neha", 28, 1500));
        employees.add(new Employee("Ravi", 35, 900));

        employees.sort(Comparator.comparingInt(Employee::getAge).thenComparing(Employee::getName));
        System.out.println("Employees sorted by age then name: " + employees);

        List<Integer> numbers = new ArrayList<>(Arrays.asList(5, 1, 3, 2, 5));
        Collections.sort(numbers, Comparator.reverseOrder());
        System.out.println("Numbers reverse sorted: " + numbers);

        /*
         * Common List interview APIs.
         */
        List<Integer> immutableList = List.of(10, 20, 30);
        System.out.println("Immutable List.of: " + immutableList);

        List<Integer> mutableList = new ArrayList<>(immutableList);
        mutableList.replaceAll(value -> value * 2);
        mutableList.removeIf(value -> value < 30);
        System.out.println("List after replaceAll/removeIf: " + mutableList);

        List<String> names = List.of("Amit", "Neha", "Ravi", "Pooja");
        String joined = String.join(", ", names);
        System.out.println("String.join: " + joined);

        System.out.println();
    }

    private static void setRevision() {
        System.out.println("2. SET REVISION");
        System.out.println("--------------");

        /*
         * HashSet:
         * - Unique elements, no guaranteed order.
         * - O(1) average add/remove/contains.
         * - Uses hashCode() and equals().
         */
        Set<String> hashSet = new HashSet<>();
        hashSet.add("Apple");
        hashSet.add("Banana");
        hashSet.add("Apple");
        hashSet.add("Orange");

        System.out.println("HashSet: " + hashSet);
        System.out.println("HashSet size: " + hashSet.size());
        System.out.println("HashSet contains Banana: " + hashSet.contains("Banana"));
        hashSet.remove("Banana");
        System.out.println("HashSet after remove: " + hashSet);

        /*
         * LinkedHashSet:
         * - Unique elements.
         * - Maintains insertion order.
         * - Slightly slower than HashSet.
         */
        Set<String> linkedHashSet = new LinkedHashSet<>();
        linkedHashSet.add("Apple");
        linkedHashSet.add("Banana");
        linkedHashSet.add("Apple");
        linkedHashSet.add("Orange");

        System.out.println("LinkedHashSet preserves insertion order: " + linkedHashSet);

        /*
         * TreeSet:
         * - Unique elements in sorted order.
         * - O(log n) add/remove/contains.
         * - Elements must be Comparable or a Comparator must be provided.
         */
        Set<String> treeSet = new TreeSet<>();
        treeSet.add("Banana");
        treeSet.add("Apple");
        treeSet.add("Orange");

        System.out.println("TreeSet sorted: " + treeSet);
        System.out.println("TreeSet first: " + ((TreeSet<String>) treeSet).first());
        System.out.println("TreeSet last: " + ((TreeSet<String>) treeSet).last());
        System.out.println("TreeSet headSet Orange: " + ((TreeSet<String>) treeSet).headSet("Orange"));

        /*
         * Set operations.
         */
        Set<Integer> setA = new HashSet<>(Arrays.asList(1, 2, 3, 4));
        Set<Integer> setB = new HashSet<>(Arrays.asList(3, 4, 5, 6));

        Set<Integer> union = new HashSet<>(setA);
        union.addAll(setB);

        Set<Integer> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);

        Set<Integer> difference = new HashSet<>(setA);
        difference.removeAll(setB);

        System.out.println("Union: " + union);
        System.out.println("Intersection: " + intersection);
        System.out.println("Difference A-B: " + difference);
        System.out.println("Is setA disjoint from setB? " + Collections.disjoint(setA, setB));

        /*
         * Important: equals and hashCode contract.
         * HashSet and HashMap depend on this contract.
         */
        Set<EmployeeKey> employeeKeys = new HashSet<>();
        employeeKeys.add(new EmployeeKey(101, "Amit"));
        employeeKeys.add(new EmployeeKey(103, "Amit"));
        employeeKeys.add(new EmployeeKey(102, "Neha"));

        System.out.println("HashSet with custom key size: " + employeeKeys.size());

        System.out.println();
    }

    private static void queueAndDequeRevision() {
        System.out.println("3. QUEUE AND DEQUE REVISION");
        System.out.println("----------------------------");

        /*
         * Queue:
         * - Usually FIFO.
         * - add/offer insert.
         * - remove/poll retrieve and remove.
         * - element/peek retrieve without removing.
         * - add/remove throw exceptions on failure.
         * - offer/poll return null or false on failure.
         */
        Queue<String> queue = new LinkedList<>();
        queue.offer("First");
        queue.offer("Second");
        queue.offer("Third");

        System.out.println("Queue: " + queue);
        System.out.println("Queue peek: " + queue.peek());
        System.out.println("Queue poll: " + queue.poll());
        System.out.println("Queue after poll: " + queue);

        /*
         * PriorityQueue:
         * - Natural ordering or custom Comparator.
         * - Does not allow null.
         * - O(log n) insertion/removal.
         * - Iteration order is not sorted.
         */
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();
        minHeap.offer(5);
        minHeap.offer(1);
        minHeap.offer(3);

        System.out.println("Min heap peek: " + minHeap.peek());
        System.out.println("Min heap poll order: " + minHeap.poll() + ", " + minHeap.poll() + ", " + minHeap.poll());

        PriorityQueue<Task> taskQueue = new PriorityQueue<>(Comparator.comparingInt(Task::getPriority));
        taskQueue.offer(new Task("Low", 3));
        taskQueue.offer(new Task("Critical", 1));
        taskQueue.offer(new Task("Medium", 2));

        System.out.println(
                "PriorityQueue tasks: " + taskQueue.poll() + " -> " + taskQueue.poll() + " -> " + taskQueue.poll());

        /*
         * Deque:
         * - Double-ended queue.
         * - Supports stack and queue behavior.
         * - Prefer ArrayDeque over Stack.
         */
        Deque<String> deque = new ArrayDeque<>();
        deque.offerFirst("Front");
        deque.offerLast("Back");
        deque.offerFirst("Very Front");

        System.out.println("Deque: " + deque);
        System.out.println("Deque pollFirst: " + deque.pollFirst());
        System.out.println("Deque pollLast: " + deque.pollLast());

        /*
         * Stack behavior using Deque.
         * java.util.Stack is legacy and synchronized.
         */
        Deque<String> stack = new ArrayDeque<>();
        stack.push("A");
        stack.push("B");
        stack.push("C");

        System.out.println("Stack via Deque: " + stack);
        System.out.println("Stack pop: " + stack.pop());
        System.out.println("Stack peek: " + stack.peek());

        System.out.println();
    }

    private static void mapRevision() {
        System.out.println("4. MAP REVISION");
        System.out.println("---------------");

        /*
         * HashMap:
         * - Key-value pairs.
         * - Allows one null key and multiple null values.
         * - O(1) average get/put/remove.
         * - No ordering guarantee.
         * - Uses hashCode() and equals() on keys.
         */
        Map<String, Integer> hashMap = new HashMap<>();
        hashMap.put("Amit", 101);
        hashMap.put("Neha", 102);
        hashMap.put("Ravi", 103);
        hashMap.put("Amit", 201);

        System.out.println("HashMap: " + hashMap);
        System.out.println("HashMap get Amit: " + hashMap.get("Amit"));
        System.out.println("HashMap containsKey Ravi: " + hashMap.containsKey("Ravi"));
        System.out.println("HashMap containsValue 102: " + hashMap.containsValue(102));
        System.out.println("HashMap getOrDefault: " + hashMap.getOrDefault("Pooja", 0));

        /*
         * Java 8 Map default methods.
         */
        hashMap.putIfAbsent("Pooja", 104);
        hashMap.computeIfAbsent("Rohan", key -> 105);
        hashMap.compute("Amit", (key, value) -> value == null ? 301 : value + 1);
        hashMap.merge("Neha", 1, Integer::sum);

        System.out.println("HashMap after default methods: " + hashMap);

        /*
         * Iterating over Map.
         * Prefer entrySet when both key and value are needed.
         */
        for (Map.Entry<String, Integer> entry : hashMap.entrySet()) {
            System.out.println("HashMap entry: " + entry.getKey() + " -> " + entry.getValue());
        }

        for (Map.Entry<String, Integer> entry : hashMap.entrySet()) {
            System.out.println("HashMap entry: " + entry.getKey() + " -> " + entry.getValue());
        }

        /*
         * LinkedHashMap:
         * - Maintains insertion order by default.
         * - Can be configured for access-order, useful for LRU cache.
         */
        Map<String, Integer> linkedHashMap = new LinkedHashMap<>();
        linkedHashMap.put("Amit", 101);
        linkedHashMap.put("Neha", 102);
        linkedHashMap.put("Ravi", 103);

        System.out.println("LinkedHashMap insertion order: " + linkedHashMap);

        /*
         * TreeMap:
         * - Sorted by key.
         * - O(log n) operations.
         * - Useful for range queries.
         */
        Map<String, Integer> treeMap = new TreeMap<>();
        treeMap.put("Banana", 2);
        treeMap.put("Apple", 1);
        treeMap.put("Orange", 3);

        System.out.println("TreeMap sorted keys: " + treeMap);
        SortedMap<String, Integer> headMap = ((TreeMap<String, Integer>) treeMap).headMap("Orange");
        System.out.println("TreeMap headMap before Orange: " + headMap);

        /*
         * IdentityHashMap:
         * - Uses reference equality == instead of equals().
         * - Rare, but useful for identity-based maps.
         */
        Map<String, String> identityMap = new IdentityHashMap<>();
        String sameValue1 = "X";
        String sameValue2 = "X".substring(0);
        identityMap.put(sameValue1, "sameValue1");
        identityMap.put(sameValue2, "sameValue2");

        System.out.println("IdentityHashMap: " + identityMap);
        System.out.println("IdentityHashMap size with same logical value but different object: " + identityMap.size());

        /*
         * Hashtable:
         * - Legacy synchronized Map.
         * - Does not allow null key or null value.
         * - Prefer ConcurrentHashMap or HashMap.
         */
        Map<String, Integer> hashtable = new Hashtable<>();
        hashtable.put("A", 1);
        hashtable.put("B", 2);

        System.out.println("Hashtable: " + hashtable);

        /*
         * ConcurrentHashMap:
         * - Thread-safe Map.
         * - Better concurrency than Hashtable because it avoids locking the whole map.
         * - Does not allow null keys or values.
         */
        Map<String, Integer> concurrentHashMap = new ConcurrentHashMap<>();
        concurrentHashMap.put("A", 1);
        concurrentHashMap.put("B", 2);
        concurrentHashMap.computeIfAbsent("C", key -> 3);
        concurrentHashMap.replace("A", 1, 10);

        System.out.println("ConcurrentHashMap: " + concurrentHashMap);

        System.out.println();
    }

    private static void sortingAndUtilityRevision() {
        System.out.println("5. SORTING AND COLLECTION UTILITY REVISION");
        System.out.println("------------------------------------------");

        List<Integer> numbers = new ArrayList<>(Arrays.asList(9, 1, 7, 3, 5));
        Collections.sort(numbers);
        System.out.println("Sorted numbers: " + numbers);

        Collections.reverse(numbers);
        System.out.println("Reversed numbers: " + numbers);

        Collections.shuffle(numbers);
        System.out.println("Shuffled numbers: " + numbers);

        int binarySearchIndex = Collections.binarySearch(numbers, numbers.get(0));
        System.out.println("Binary search on sorted list requires sorted input. Example index: " + binarySearchIndex);

        List<Integer> synchronizedList = Collections.synchronizedList(new ArrayList<>());
        synchronizedList.add(10);
        synchronizedList.add(20);
        System.out.println("SynchronizedList: " + synchronizedList);

        List<Integer> unmodifiableList = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(1, 2, 3)));
        System.out.println("UnmodifiableList: " + unmodifiableList);

        /*
         * Java 9+ List.of/Set.of/Map.of:
         * - Immutable collections.
         * - No null elements.
         * - Not suitable when mutation is required.
         */
        List<String> immutableNames = List.of("Amit", "Neha", "Ravi");
        Set<String> immutableSet = Set.of("A", "B", "C");
        Map<String, Integer> immutableMap = Map.of("A", 1, "B", 2);

        System.out.println("Immutable List: " + immutableNames);
        System.out.println("Immutable Set: " + immutableSet);
        System.out.println("Immutable Map: " + immutableMap);

        /*
         * Stream APIs commonly used with collections.
         */
        List<Employee> employees = new ArrayList<>();
        employees.add(new Employee("Amit", 35, 1000));
        employees.add(new Employee("Neha", 28, 1500));
        employees.add(new Employee("Ravi", 35, 900));
        employees.add(new Employee("Pooja", 40, 2000));

        List<String> names = employees.stream()
                .filter(employee -> employee.getSalary() > 1000)
                .sorted(Comparator.comparing(Employee::getName))
                .map(Employee::getName)
                .collect(Collectors.toList());

        System.out.println("Stream filtered names: " + names);

        Map<Integer, List<Employee>> employeesByAge = employees.stream()
                .collect(Collectors.groupingBy(Employee::getAge));

        System.out.println("Employees grouped by age: " + employeesByAge);

        Optional<Employee> highestPaid = employees.stream()
                .max(Comparator.comparingInt(Employee::getSalary));

        System.out.println("Highest paid: " + highestPaid);

        List<Integer> distinctNumbers = IntStream.rangeClosed(1, 5)
                .boxed()
                .collect(Collectors.toList());

        System.out.println("IntStream range: " + distinctNumbers);

        System.out.println();
    }

    private static void concurrentCollectionsRevision() {
        System.out.println("6. CONCURRENT COLLECTIONS REVISION");
        System.out.println("----------------------------------");

        /*
         * ConcurrentHashMap:
         * - Thread-safe Map.
         * - Null keys/values are not allowed.
         * - Supports atomic operations like compute, merge, putIfAbsent.
         */
        ConcurrentHashMap<String, Integer> concurrentHashMap = new ConcurrentHashMap<>();
        concurrentHashMap.put("A", 1);
        concurrentHashMap.put("B", 2);
        concurrentHashMap.compute("A", (key, value) -> value == null ? 1 : value + 1);
        System.out.println("ConcurrentHashMap after compute: " + concurrentHashMap);

        /*
         * CopyOnWriteArrayList:
         * - Thread-safe List.
         * - Safe iteration without ConcurrentModificationException.
         * - Best for read-heavy workloads.
         */
        CopyOnWriteArrayList<String> copyOnWriteArrayList = new CopyOnWriteArrayList<>();
        copyOnWriteArrayList.add("A");
        copyOnWriteArrayList.add("B");
        System.out.println("CopyOnWriteArrayList: " + copyOnWriteArrayList);

        /*
         * ConcurrentLinkedQueue:
         * - Non-blocking thread-safe Queue.
         * - Good for producer-consumer style workloads without blocking.
         */
        Queue<String> concurrentLinkedQueue = new ConcurrentLinkedQueue<>();
        concurrentLinkedQueue.offer("A");
        concurrentLinkedQueue.offer("B");
        System.out.println("ConcurrentLinkedQueue: " + concurrentLinkedQueue);

        /*
         * BlockingQueue:
         * - Thread-safe Queue with blocking operations.
         * - put blocks if queue is full.
         * - take blocks if queue is empty.
         */
        BlockingQueue<String> blockingQueue = new LinkedBlockingQueue<>(2);
        blockingQueue.offer("Task-1");
        blockingQueue.offer("Task-2");
        boolean inserted = blockingQueue.offer("Task-3");

        System.out.println("BlockingQueue offer result when full: " + inserted);
        System.out.println("BlockingQueue: " + blockingQueue);

        /*
         * PriorityBlockingQueue:
         * - Thread-safe PriorityQueue.
         * - Unbounded.
         */
        PriorityBlockingQueue<Integer> priorityBlockingQueue = new PriorityBlockingQueue<>();
        priorityBlockingQueue.offer(5);
        priorityBlockingQueue.offer(1);
        priorityBlockingQueue.offer(3);
        System.out.println("PriorityBlockingQueue: " + priorityBlockingQueue);

        System.out.println();
    }

    private static void legacyCollectionsRevision() {
        System.out.println("7. LEGACY COLLECTIONS REVISION");
        System.out.println("------------------------------");

        /*
         * Vector:
         * - Synchronized dynamic array.
         * - Legacy.
         * - Prefer ArrayList or CopyOnWriteArrayList.
         */
        Vector<String> vector = new Vector<>();
        vector.add("Legacy-1");
        vector.addElement("Legacy-2");
        System.out.println("Vector: " + vector);

        /*
         * Stack:
         * - Legacy synchronized LIFO structure.
         * - Prefer ArrayDeque for stack behavior.
         */
        Stack<String> stack = new Stack<>();
        stack.push("A");
        stack.push("B");
        stack.push("C");
        System.out.println("Legacy Stack: " + stack);
        System.out.println("Legacy Stack pop: " + stack.pop());

        /*
         * Hashtable:
         * - Legacy synchronized Map.
         * - No null key/value.
         * - Prefer ConcurrentHashMap or HashMap.
         */
        Hashtable<String, Integer> hashtable = new Hashtable<>();
        hashtable.put("A", 1);
        hashtable.put("B", 2);
        System.out.println("Legacy Hashtable: " + hashtable);

        System.out.println();
    }

    private static void interviewImportantPatterns() {
        System.out.println("8. INTERVIEW IMPORTANT PATTERNS");
        System.out.println("-------------------------------");

        /*
         * Pattern 1: Count frequency using HashMap.
         * Common in questions like first non-repeating character, majority element,
         * duplicate count.
         */
        String text = "interview";
        Map<Character, Integer> frequencyMap = new HashMap<>();

        for (char ch : text.toCharArray()) {
            frequencyMap.put(ch, frequencyMap.getOrDefault(ch, 0) + 1);
        }

        System.out.println("Character frequency: " + frequencyMap);

        /*
         * Pattern 2: Remove duplicates while preserving order.
         * LinkedHashSet is ideal because it keeps insertion order.
         */
        List<String> input = new ArrayList<>(Arrays.asList("A", "B", "A", "C", "B", "D"));
        List<String> uniqueInOrder = new ArrayList<>(new LinkedHashSet<>(input));
        System.out.println("Unique preserving order: " + uniqueInOrder);

        /*
         * Pattern 3: Find two numbers with target sum.
         * HashMap gives O(n) time complexity.
         */
        int[] nums = { 2, 7, 11, 15, 3 };
        int target = 10;
        int[] twoSum = findTwoSum(nums, target);
        System.out.println("Two sum indices: " + Arrays.toString(twoSum));

        /*
         * Pattern 4: LRU cache skeleton using LinkedHashMap.
         * removeEldestEntry controls maximum size.
         */
        LRUCache<String, String> lruCache = new LRUCache<>(4);
        lruCache.put("A", "1");
        lruCache.put("B", "2");
        lruCache.put("C", "3");
        lruCache.get("A");
        lruCache.put("D", "4");
        System.out.println("LRU cache: " + lruCache);

        /*
         * Pattern 5: Top K frequent elements using HashMap + PriorityQueue.
         * This is a common senior-level data-structure pattern.
         */
        List<String> words = Arrays.asList("apple", "banana", "apple", "orange", "banana", "apple");
        Map<String, Integer> wordFrequency = new HashMap<>();
        for (String word : words) {
            wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
        }

        PriorityQueue<String> topK = new PriorityQueue<>(
                (a, b) -> wordFrequency.get(a) - wordFrequency.get(b));

        for (String word : wordFrequency.keySet()) {
            topK.offer(word);
            if (topK.size() > 2) {
                topK.poll();
            }
        }

        System.out.println("Top 2 frequent words: " + topK);

        /*
         * Pattern 6: Sliding window using ArrayDeque.
         * Deque stores indices of useful elements.
         */
        int[] slidingWindow = { 1, 3, -1, -3, 5, 3, 6, 7 };
        System.out.println("Sliding window max 3: " + Arrays.toString(slidingWindowMax(slidingWindow, 3)));

        /*
         * Pattern 7: Merge intervals using sorting.
         */
        List<int[]> intervals = new ArrayList<>();
        intervals.add(new int[] { 1, 3 });
        intervals.add(new int[] { 2, 6 });
        intervals.add(new int[] { 8, 10 });
        intervals.add(new int[] { 15, 18 });

        System.out.println("Merged intervals: " + mergeIntervals(intervals));

        System.out.println();
    }

    private static int[] findTwoSum(int[] nums, int target) {
        Map<Integer, Integer> indexByValue = new HashMap<>();

        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];
            if (indexByValue.containsKey(complement)) {
                return new int[] { indexByValue.get(complement), i };
            }
            indexByValue.put(nums[i], i);
        }

        return new int[] { -1, -1 };
    }

    private static int[] slidingWindowMax(int[] nums, int k) {
        if (nums == null || nums.length == 0 || k <= 0) {
            return new int[0];
        }

        int[] result = new int[nums.length - k + 1];
        Deque<Integer> deque = new ArrayDeque<>();

        for (int i = 0; i < nums.length; i++) {
            while (!deque.isEmpty() && deque.peekFirst() < i - k + 1) {
                deque.pollFirst();
            }

            while (!deque.isEmpty() && nums[deque.peekLast()] < nums[i]) {
                deque.pollLast();
            }

            deque.offerLast(i);

            if (i >= k - 1) {
                result[i - k + 1] = nums[deque.peekFirst()];
            }
        }

        return result;
    }

    private static List<int[]> mergeIntervals(List<int[]> intervals) {
        if (intervals == null || intervals.isEmpty()) {
            return new ArrayList<>();
        }

        intervals.sort(Comparator.comparingInt(interval -> interval[0]));

        List<int[]> merged = new ArrayList<>();
        int[] current = intervals.get(0);

        for (int i = 1; i < intervals.size(); i++) {
            int[] next = intervals.get(i);

            if (next[0] <= current[1]) {
                current[1] = Math.max(current[1], next[1]);
            } else {
                merged.add(current);
                current = next;
            }
        }

        merged.add(current);
        return merged;
    }

    private static final class Employee {
        private final String name;
        private final int age;
        private final int salary;

        private Employee(String name, int age, int salary) {
            this.name = name;
            this.age = age;
            this.salary = salary;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        public int getSalary() {
            return salary;
        }

        @Override
        public String toString() {
            return name + "(" + age + ", " + salary + ")";
        }
    }

    private static final class EmployeeKey {
        private final int id;
        private final String name;

        private EmployeeKey(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof EmployeeKey)) {
                return false;
            }
            EmployeeKey that = (EmployeeKey) object;
            return id == that.id && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }

        @Override
        public String toString() {
            return "EmployeeKey{" + "id=" + id + ", name='" + name + '\'' + '}';
        }
    }

    private static final class Task implements Comparable<Task> {
        private final String name;
        private final int priority;

        private Task(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }

        @Override
        public int compareTo(Task other) {
            return Integer.compare(this.priority, other.priority);
        }

        @Override
        public String toString() {
            return name + "[" + priority + "]";
        }
    }

    private static final class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int capacity;

        private LRUCache(int capacity) {
            super(capacity, 0.75f, true);
            this.capacity = capacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;
        }
    }
}

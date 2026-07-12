package LLD;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// =========================
// 1. Cache Contract
// =========================
interface Cache<K, V> {
    Optional<V> get(K key);

    void put(K key, V value);

    void remove(K key);

    int size();

    int capacity();
}

// =========================
// 2. Storage Contract
// =========================
interface Storage<K, V> {
    Optional<V> get(K key);

    void put(K key, V value);

    void remove(K key);

    boolean contains(K key);

    int size();
}

// =========================
// 3. Eviction Policy Contract
// =========================
interface EvictionPolicy<K> {
    void keyAccessed(K key);

    void keyRemoved(K key);

    K evictKey();
}

// =========================
// 4. In-Memory Storage
// =========================
class HashMapStorage<K, V> implements Storage<K, V> {
    private final Map<K, V> map = new HashMap<>();

    @Override
    public synchronized Optional<V> get(K key) {
        return Optional.ofNullable(map.get(key));
    }

    @Override
    public synchronized void put(K key, V value) {
        map.put(key, value);
    }

    @Override
    public synchronized void remove(K key) {
        map.remove(key);
    }

    @Override
    public synchronized boolean contains(K key) {
        return map.containsKey(key);
    }

    @Override
    public synchronized int size() {
        return map.size();
    }
}

// =========================
// 5. Doubly Linked List Node
// =========================
class DoublyLinkedListNode<K> {
    K key;
    DoublyLinkedListNode<K> prev;
    DoublyLinkedListNode<K> next;

    DoublyLinkedListNode(K key) {
        this.key = key;
    }
}

// =========================
// 6. LRU Eviction Policy
// =========================
class LRUEvictionPolicy<K> implements EvictionPolicy<K> {
    private final Map<K, DoublyLinkedListNode<K>> nodeMap;
    private final DoublyLinkedListNode<K> head;
    private final DoublyLinkedListNode<K> tail;

    public LRUEvictionPolicy() {
        this.nodeMap = new HashMap<>();
        this.head = new DoublyLinkedListNode<>(null); // dummy head
        this.tail = new DoublyLinkedListNode<>(null); // dummy tail
        head.next = tail;
        tail.prev = head;
    }

    @Override
    public synchronized void keyAccessed(K key) {
        if (nodeMap.containsKey(key)) {
            DoublyLinkedListNode<K> node = nodeMap.get(key);
            removeNode(node);
            addToFront(node);
        } else {
            DoublyLinkedListNode<K> newNode = new DoublyLinkedListNode<>(key);
            nodeMap.put(key, newNode);
            addToFront(newNode);
        }
    }

    @Override
    public synchronized void keyRemoved(K key) {
        DoublyLinkedListNode<K> node = nodeMap.remove(key);
        if (node != null) {
            removeNode(node);
        }
    }

    @Override
    public synchronized K evictKey() {
        if (tail.prev == head) {
            throw new IllegalStateException("No keys available for eviction");
        }

        DoublyLinkedListNode<K> lruNode = tail.prev;
        removeNode(lruNode);
        nodeMap.remove(lruNode.key);
        return lruNode.key;
    }

    private void addToFront(DoublyLinkedListNode<K> node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }

    private void removeNode(DoublyLinkedListNode<K> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }
}

// =========================
// 7. LRU Cache Implementation
// =========================
class LRUCache<K, V> implements Cache<K, V> {
    private final int capacity;
    private final Storage<K, V> storage;
    private final EvictionPolicy<K> evictionPolicy;

    public LRUCache(int capacity, Storage<K, V> storage, EvictionPolicy<K> evictionPolicy) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0");
        }
        this.capacity = capacity;
        this.storage = storage;
        this.evictionPolicy = evictionPolicy;
    }

    @Override
    public synchronized Optional<V> get(K key) {
        Optional<V> value = storage.get(key);
        value.ifPresent(v -> evictionPolicy.keyAccessed(key));
        return value;
    }

    @Override
    public synchronized void put(K key, V value) {
        if (storage.contains(key)) {
            storage.put(key, value);
            evictionPolicy.keyAccessed(key);
            return;
        }

        if (storage.size() >= capacity) {
            K keyToEvict = evictionPolicy.evictKey();
            storage.remove(keyToEvict);
        }

        storage.put(key, value);
        evictionPolicy.keyAccessed(key);
    }

    @Override
    public synchronized void remove(K key) {
        if (storage.contains(key)) {
            storage.remove(key);
            evictionPolicy.keyRemoved(key);
        }
    }

    @Override
    public synchronized int size() {
        return storage.size();
    }

    @Override
    public synchronized int capacity() {
        return capacity;
    }
}

// =========================
// 8. Demo
// =========================
public class LRUCacheLLD {
    public static void main(String[] args) {
        Cache<Integer, String> cache = new LRUCache<>(3, new HashMapStorage<>(), new LRUEvictionPolicy<>());

        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");

        System.out.println(cache.get(1)); // Optional[A], key 1 becomes most recently used
        cache.put(4, "D"); // evicts key 2

        System.out.println(cache.get(2)); // Optional.empty
        System.out.println(cache.get(3)); // Optional[C]
        System.out.println(cache.get(4)); // Optional[D]
        System.out.println(cache.get(1)); // Optional[A]
    }
}
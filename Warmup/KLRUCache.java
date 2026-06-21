package Warmup;

import java.util.HashMap;
import java.util.Map;

public class KLRUCache {
    static class Node {
        int key;
        int value;
        Node next;
        Node prev;

        Node(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }

    static class LRUCache {
        private final int capacity;
        private final Map<Integer, Node> cache;
        private final Node head;
        private final Node tail;

        public LRUCache(int capacity) {
            this.capacity = capacity;
            this.cache = new HashMap<>();
            this.head = new Node(0, 0);
            this.tail = new Node(0, 0);
            head.next = tail;
            tail.prev = head;
        }

        public int get(int key) {
            if (!cache.containsKey(key)) {
                return -1;
            }

            Node node = cache.get(key);
            remove(node);
            insertAtFront(node);
            return node.value;
        }

        public void put(int key, int value) {
            if (cache.containsKey(key)) {
                Node existing = cache.get(key);
                existing.value = value;
                remove(existing);
                insertAtFront(existing);
                return;
            }

            if (cache.size() == capacity) {
                Node lru = tail.prev;
                remove(lru);
                cache.remove(lru.key);
            }

            Node newNode = new Node(key, value);
            insertAtFront(newNode);
            cache.put(key, newNode);
        }

        private void remove(Node node) {
            node.prev.next = node.next;
            node.next.prev = node.prev;
        }

        private void insertAtFront(Node node) {
            node.next = head.next;
            node.prev = head;

            head.next.prev = node;
            head.next = node;
        }

        @Override
        public String toString() {
            return "LRUCache [capacity=" + capacity + ", cache=" + cache + "]";
        }

    }

    public static void main(String[] args) {
        LRUCache cache = new LRUCache(2);
        cache.put(1, 10);
        cache.put(2, 20);
        cache.put(3, 30); // evicts key 2
        System.out.print(cache);
        System.out.println(cache.get(2)); // -1
    }

}

package LLD;

import java.util.HashMap;
import java.util.Map;

public class LRUCacheSimpleLLD {

    static class Node {
        int key;
        int value;
        Node prev;
        Node next;

        Node(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }

    static class LRUCache {
        private final int capacity;
        private final Map<Integer, Node> map;
        private final Node head; // dummy head (most recently used side)
        private final Node tail; // dummy tail (least recently used side)

        LRUCache(int capacity) {
            this.capacity = capacity;
            this.map = new HashMap<>();
            this.head = new Node(-1, -1);
            this.tail = new Node(-1, -1);
            head.next = tail;
            tail.prev = head;
        }

        int get(int key) {
            if (!map.containsKey(key)) {
                return -1;
            }
            Node node = map.get(key);
            remove(node);
            insertAtFront(node);
            return node.value;
        }

        void put(int key, int value) {
            if (map.containsKey(key)) {
                Node existing = map.get(key);
                remove(existing);
            } else if (map.size() == capacity) {
                Node lru = tail.prev;
                remove(lru);
                map.remove(lru.key);
            }
            Node node = new Node(key, value);
            insertAtFront(node);
            map.put(key, node);
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
    }

    public static void main(String[] args) {
        LRUCache cache = new LRUCache(2);

        cache.put(1, 10);
        cache.put(2, 20);
        System.out.println(cache.get(1)); // 10

        cache.put(3, 30); // evicts key 2 (least recently used)
        System.out.println(cache.get(2)); // -1

        cache.put(4, 40); // evicts key 1
        System.out.println(cache.get(1)); // -1
        System.out.println(cache.get(3)); // 30
        System.out.println(cache.get(4)); // 40
    }
}

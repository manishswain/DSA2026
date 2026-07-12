package InterviewQuestions;

public class Esper {

import java.io.*;
import java.util.*;

/*
 * To execute Java, please define "static void main" on a class
 * named Solution.
 *
 * If you need more classes, simply define them inline.
 */

// class Solution {
// public static void main(String[] args) {
// String s = "abbc";
// int res = answer(s);
// System.out.println(res);
// }

// private static int answer(String s){
// if(s.length()==0){
// return 0;
// }
// int i=0,j=0;
// Set<Character> set = new HashSet<>();
// while(i<=j && j< s.length()){
// if(!set.contains(s.charAt(j))){
// set.add(s.charAt(j));
// j++;
// // System.out.println(set);
// }else{
// set.remove(s.charAt(j));
// i++;
// // System.out.println(set);
// }
// }
// return set.size();
// }
// }

// abcabcbb

interface Cache {
    int capacity();
}

class LRUCache implements Cache {
    private int capacity;
    private List<Node> list;
    private Map<Integer, Node> map;
    private Node next;
    private Node prev;

    public LRUCache(int capacity) {
        this.map = new HashMap<>();
        this.list = new LinkedList<>();
        this.capacity = capacity;
        this.next = new Node(0);
        this.prev = new Node(0);

    }

    void insert(int key, int value) {
        if (map.contains(key)) {
            Node n = new Node(key, value);
            map.put(key, n);
        } else {

        }
    }

    Node remove(Node n) {

    }

    void moveToFirst(Node n) {

    }

}

class Node {
    int key;
    int value;
    Node next;
    Node prev;

    Node(int key, int value) {
        this.key = key;
        this.value = value;
    }
}

}

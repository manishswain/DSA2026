package Namaste.StackAndQueue;

import java.util.Stack;

public class MinStack155 {
    public static void main(String[] args) {

    }
}

class MinStack {

    private Stack<Long[]> stack;

    public MinStack() {
        stack = new Stack<>();
    }

    public void push(long value) {
        if (stack.isEmpty()) {
            this.stack.push(new Long[] { value, value });
        } else {
            long minVal = Math.min(value, stack.peek()[1]);
            this.stack.push(new Long[] { value, minVal });
        }
    }

    public void pop() {
        this.stack.pop();
    }

    public long top() {
        return this.stack.peek()[0];
    }

    public long getMin() {
        return this.stack.peek()[1];
    }
}
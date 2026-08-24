package Namaste.Arrays;

import java.util.Arrays;

public class MeetingRoomsII {
    public static void main(String[] args) {
        int[][] intervals = { { 0, 30 }, { 5, 10 }, { 15, 20 } };

        int result = minMeetingRooms(intervals);
        System.out.println("Minimum Meeting Rooms: " + result);
    }

    private static int minMeetingRooms(int[][] intervals) {
        int n = intervals.length;
        int[] start = new int[n];
        int[] end = new int[n];

        for (int i = 0; i < n; i++) {
            start[i] = intervals[i][0];
            end[i] = intervals[i][1];
        }

        Arrays.sort(start);
        Arrays.sort(end);

        int rooms = 0, maxRooms = 0;
        int s = 0, e = 0;

        while (s < n) {
            if (start[s] < end[e]) {
                rooms++;
                s++;
            } else {
                rooms--;
                e++;
            }

            maxRooms = Math.max(maxRooms, rooms);
        }

        return maxRooms;
    }
}

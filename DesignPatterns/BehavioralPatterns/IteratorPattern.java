package DesignPatterns.BehavioralPatterns;

import java.util.ArrayList;
import java.util.List;

// Song class
class Song {
    private String title;
    private String artist;
    private int duration; // in seconds

    public Song(String title, String artist, int duration) {
        this.title = title;
        this.artist = artist;
        this.duration = duration;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public int getDuration() {
        return duration;
    }

    @Override
    public String toString() {
        return "Song{" +
                "title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", duration=" + duration + "s" +
                '}';
    }
}

// Iterator interface
interface PlaylistIterator {
    boolean hasNext();

    Song next();

    void remove();
}

// Concrete Iterator
class PlaylistIteratorImpl implements PlaylistIterator {
    private List<Song> songs;
    private int currentIndex = 0;

    public PlaylistIteratorImpl(List<Song> songs) {
        this.songs = songs;
    }

    @Override
    public boolean hasNext() {
        return currentIndex < songs.size();
    }

    @Override
    public Song next() {
        if (!hasNext()) {
            throw new IllegalStateException("No more songs in the playlist");
        }
        return songs.get(currentIndex++);
    }

    @Override
    public void remove() {
        if (currentIndex == 0) {
            throw new IllegalStateException("Cannot remove before calling next()");
        }
        songs.remove(--currentIndex);
    }
}

// Reverse Iterator
class ReversePlaylistIterator implements PlaylistIterator {
    private List<Song> songs;
    private int currentIndex;

    public ReversePlaylistIterator(List<Song> songs) {
        this.songs = songs;
        this.currentIndex = songs.size();
    }

    @Override
    public boolean hasNext() {
        return currentIndex > 0;
    }

    @Override
    public Song next() {
        if (!hasNext()) {
            throw new IllegalStateException("No more songs in the playlist");
        }
        return songs.get(--currentIndex);
    }

    @Override
    public void remove() {
        songs.remove(currentIndex);
    }
}

// Collection interface
interface Iterable {
    PlaylistIterator createIterator();
}

// Concrete Collection: MusicPlaylist
class MusicPlaylist implements Iterable {
    private String playlistName;
    private List<Song> songs = new ArrayList<>();

    public MusicPlaylist(String playlistName) {
        this.playlistName = playlistName;
    }

    public void addSong(Song song) {
        songs.add(song);
        System.out.println("Added: " + song.getTitle() + " to " + playlistName);
    }

    public void removeSong(Song song) {
        songs.remove(song);
        System.out.println("Removed: " + song.getTitle() + " from " + playlistName);
    }

    public int getTotalDuration() {
        int total = 0;
        for (Song song : songs) {
            total += song.getDuration();
        }
        return total;
    }

    public int getPlaylistSize() {
        return songs.size();
    }

    @Override
    public PlaylistIterator createIterator() {
        return new PlaylistIteratorImpl(songs);
    }

    public PlaylistIterator createReverseIterator() {
        return new ReversePlaylistIterator(songs);
    }
}

// Demo class
public class IteratorPattern {
    public static void main(String[] args) {
        // Create a music playlist
        MusicPlaylist myPlaylist = new MusicPlaylist("My Favorite Songs");

        // Add songs to the playlist
        System.out.println("=== Adding Songs to Playlist ===");
        myPlaylist.addSong(new Song("Bohemian Rhapsody", "Queen", 354));
        myPlaylist.addSong(new Song("Imagine", "John Lennon", 183));
        myPlaylist.addSong(new Song("Hotel California", "Eagles", 391));
        myPlaylist.addSong(new Song("Stairway to Heaven", "Led Zeppelin", 482));
        myPlaylist.addSong(new Song("Smells Like Teen Spirit", "Nirvana", 301));

        // Forward iteration through playlist
        System.out.println("\n=== Forward Iteration ===");
        PlaylistIterator forwardIterator = myPlaylist.createIterator();
        while (forwardIterator.hasNext()) {
            Song song = forwardIterator.next();
            System.out.println(song);
        }

        // Reverse iteration through playlist
        System.out.println("\n=== Reverse Iteration ===");
        PlaylistIterator reverseIterator = myPlaylist.createReverseIterator();
        while (reverseIterator.hasNext()) {
            Song song = reverseIterator.next();
            System.out.println(song);
        }

        // Display playlist statistics
        System.out.println("\n=== Playlist Statistics ===");
        System.out.println("Playlist: " + myPlaylist.getPlaylistSize() + " songs");
        System.out.println("Total Duration: " + myPlaylist.getTotalDuration() + " seconds");
        System.out.println("Total Duration: " + (myPlaylist.getTotalDuration() / 60) + " minutes");
    }
}

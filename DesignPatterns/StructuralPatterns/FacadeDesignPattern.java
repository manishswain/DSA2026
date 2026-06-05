package DesignPatterns.StructuralPatterns;

// ============================================================
// MULTIMEDIA SYSTEM - FACADE DESIGN PATTERN
// ============================================================
// Problem: A multimedia app has many complex subsystems
// (MusicPlayer, VideoPlayer, ImageViewer) with low-level APIs.
// The client (MultimediaApp facade user) wants a simple,
// unified interface to perform common actions like
// "play music", "play video", "view image", "stop everything".
// The Facade hides the complexity of the subsystems.
// ============================================================

// ----- Subsystem 1: Music Player -----
class MusicPlayer {
    private boolean isPlaying = false;
    private String currentTrack = null;

    public void loadTrack(String track) {
        System.out.println("[MusicPlayer] Loading track: " + track);
        this.currentTrack = track;
    }

    public void decodeAudio() {
        System.out.println("[MusicPlayer] Decoding audio stream...");
    }

    public void startPlayback() {
        isPlaying = true;
        System.out.println("[MusicPlayer] Playing: " + currentTrack);
    }

    public void stop() {
        isPlaying = false;
        System.out.println("[MusicPlayer] Stopped");
    }
}

// ----- Subsystem 2: Video Player -----
class VideoPlayer {
    private boolean isPlaying = false;
    private String currentVideo = null;

    public void loadVideo(String video) {
        System.out.println("[VideoPlayer] Loading video: " + video);
        this.currentVideo = video;
    }

    public void decodeVideo() {
        System.out.println("[VideoPlayer] Decoding video frames...");
    }

    public void renderVideo() {
        System.out.println("[VideoPlayer] Rendering video to screen...");
    }

    public void startPlayback() {
        isPlaying = true;
        System.out.println("[VideoPlayer] Playing: " + currentVideo);
    }

    public void stop() {
        isPlaying = false;
        System.out.println("[VideoPlayer] Stopped");
    }
}

// ----- Subsystem 3: Image Viewer -----
class ImageViewer {
    private boolean isShowing = false;
    private String currentImage = null;

    public void loadImage(String image) {
        System.out.println("[ImageViewer] Loading image: " + image);
        this.currentImage = image;
    }

    public void applyFilters() {
        System.out.println("[ImageViewer] Applying image filters...");
    }

    public void display() {
        isShowing = true;
        System.out.println("[ImageViewer] Displaying: " + currentImage);
    }

    public void close() {
        isShowing = false;
        System.out.println("[ImageViewer] Closed");
    }
}

// ----- FACADE: Simple unified interface for the client -----
class MultimediaFacade {
    private final MusicPlayer musicPlayer;
    private final VideoPlayer videoPlayer;
    private final ImageViewer imageViewer;

    public MultimediaFacade() {
        this.musicPlayer = new MusicPlayer();
        this.videoPlayer = new VideoPlayer();
        this.imageViewer = new ImageViewer();
    }

    // High-level: just give a track name, facade handles the rest
    public void playMusic(String track) {
        System.out.println("\n=== FACADE: Play Music ===");
        musicPlayer.loadTrack(track);
        musicPlayer.decodeAudio();
        musicPlayer.startPlayback();
    }

    // High-level: just give a video name, facade handles the rest
    public void playVideo(String video) {
        System.out.println("\n=== FACADE: Play Video ===");
        videoPlayer.loadVideo(video);
        videoPlayer.decodeVideo();
        videoPlayer.renderVideo();
        videoPlayer.startPlayback();
    }

    // High-level: just give an image name, facade handles the rest
    public void viewImage(String image) {
        System.out.println("\n=== FACADE: View Image ===");
        imageViewer.loadImage(image);
        imageViewer.applyFilters();
        imageViewer.display();
    }

    // One-call action: stop everything
    public void stopAll() {
        System.out.println("\n=== FACADE: Stop All ===");
        musicPlayer.stop();
        videoPlayer.stop();
        imageViewer.close();
    }
}

// ----- Client Demo -----
public class FacadeDesignPattern {
    public static void main(String[] args) {
        // Client only knows about the Facade
        MultimediaFacade multimedia = new MultimediaFacade();

        // Simple, unified calls - subsystem complexity is hidden
        multimedia.playMusic("song.mp3");
        multimedia.playVideo("movie.mp4");
        multimedia.viewImage("photo.jpg");

        // One call to stop everything
        multimedia.stopAll();
    }
}

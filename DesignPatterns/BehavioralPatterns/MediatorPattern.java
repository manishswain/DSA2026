package DesignPatterns.BehavioralPatterns;

import java.util.HashMap;
import java.util.Map;

// Mediator interface
interface AuctionMediator {
    void registerBidder(Bidder bidder);

    void unregisterBidder(Bidder bidder);

    void placeBid(Bidder bidder, double amount);

    void announceHighestBid();
}

// Concrete Mediator: AuctionHouse
class AuctionHouse implements AuctionMediator {
    private String item;
    private double currentHighestBid = 0;
    private Bidder highestBidder;
    private Map<String, Bidder> bidders = new HashMap<>();

    public AuctionHouse(String item) {
        this.item = item;
    }

    @Override
    public void registerBidder(Bidder bidder) {
        bidders.put(bidder.getName(), bidder);
        bidder.setMediator(this);
        System.out.println(bidder.getName() + " registered for auction of: " + item);
    }

    @Override
    public void unregisterBidder(Bidder bidder) {
        bidders.remove(bidder.getName());
        System.out.println(bidder.getName() + " unregistered from auction");
    }

    @Override
    public void placeBid(Bidder bidder, double amount) {
        if (amount > currentHighestBid) {
            if (highestBidder != null) {
                System.out.println("Outbid notification sent to: " + highestBidder.getName());
                highestBidder.receiveBidUpdate("You were outbid! New highest bid: $" + amount);
            }
            currentHighestBid = amount;
            highestBidder = bidder;
            System.out.println(bidder.getName() + " placed bid: $" + amount);
        } else {
            System.out.println("Bid rejected for " + bidder.getName() + ": $" + amount + " (Current highest: $"
                    + currentHighestBid + ")");
            bidder.receiveBidUpdate(
                    "Your bid of $" + amount + " is not high enough. Current highest: $" + currentHighestBid);
        }
    }

    @Override
    public void announceHighestBid() {
        System.out.println("\n===== AUCTION RESULTS =====");
        System.out.println("Item: " + item);
        if (highestBidder != null) {
            System.out.println("Winner: " + highestBidder.getName());
            System.out.println("Winning Bid: $" + currentHighestBid);
        } else {
            System.out.println("No bids placed for this item");
        }
    }

    public double getCurrentHighestBid() {
        return currentHighestBid;
    }

    public Bidder getHighestBidder() {
        return highestBidder;
    }
}

// Colleague interface
interface Bidder {
    void placeBid(double amount);

    void receiveBidUpdate(String message);

    String getName();

    void setMediator(AuctionMediator mediator);
}

// Concrete Colleague: Participant
class Participant implements Bidder {
    private String name;
    private AuctionMediator mediator;
    private double maxBudget;

    public Participant(String name, double maxBudget) {
        this.name = name;
        this.maxBudget = maxBudget;
    }

    @Override
    public void setMediator(AuctionMediator mediator) {
        this.mediator = mediator;
    }

    @Override
    public void placeBid(double amount) {
        if (amount <= maxBudget) {
            mediator.placeBid(this, amount);
        } else {
            System.out.println(name + " cannot bid $" + amount + " (Budget: $" + maxBudget + ")");
        }
    }

    @Override
    public void receiveBidUpdate(String message) {
        System.out.println(name + " received update: " + message);
    }

    @Override
    public String getName() {
        return name;
    }
}

// Extended Mediator with Logging and Time Limit
class TimeLimitedAuctionHouse extends AuctionHouse {
    private long auctionStartTime;
    private long timeLimitSeconds;
    private StringBuilder bidLog;

    public TimeLimitedAuctionHouse(String item, long timeLimitSeconds) {
        super(item);
        this.timeLimitSeconds = timeLimitSeconds;
        this.auctionStartTime = System.currentTimeMillis();
        this.bidLog = new StringBuilder();
        logMessage("Auction started with " + timeLimitSeconds + " seconds time limit");
    }

    private void logMessage(String message) {
        String logEntry = "[LOG] " + message;
        bidLog.append(logEntry).append("\n");
        System.out.println(logEntry);
    }

    private boolean isAuctionActive() {
        long elapsedSeconds = (System.currentTimeMillis() - auctionStartTime) / 1000;
        return elapsedSeconds < timeLimitSeconds;
    }

    private long getTimeRemaining() {
        long elapsedSeconds = (System.currentTimeMillis() - auctionStartTime) / 1000;
        return timeLimitSeconds - elapsedSeconds;
    }

    @Override
    public void placeBid(Bidder bidder, double amount) {
        if (!isAuctionActive()) {
            logMessage("AUCTION CLOSED: Bid rejected for " + bidder.getName() +
                    ". Time limit exceeded!");
            bidder.receiveBidUpdate("Auction has ended! No more bids accepted.");
            return;
        }

        long timeRemaining = getTimeRemaining();
        logMessage("Bid attempt by " + bidder.getName() + ": $" + amount +
                " (Time remaining: " + timeRemaining + "s)");

        if (amount > getCurrentHighestBid()) {
            if (getHighestBidder() != null) {
                logMessage("Outbid notification: " + getHighestBidder().getName());
                getHighestBidder().receiveBidUpdate("You were outbid! New highest bid: $" + amount);
            }
            logMessage("BID ACCEPTED: " + bidder.getName() + " placed $" + amount);
            super.placeBid(bidder, amount);
        } else {
            logMessage("BID REJECTED: " + bidder.getName() + " - $" + amount +
                    " not higher than current highest: $" + getCurrentHighestBid());
            bidder.receiveBidUpdate("Your bid of $" + amount + " is not high enough. Current highest: $" +
                    getCurrentHighestBid());
        }
    }

    @Override
    public void announceHighestBid() {
        logMessage("=== ANNOUNCING AUCTION RESULTS ===");
        super.announceHighestBid();
    }

    public void printBidLog() {
        System.out.println("\n===== BID LOG =====");
        System.out.println(bidLog.toString());
    }

    public boolean isTimeExpired() {
        return !isAuctionActive();
    }
}

// Demo class
public class MediatorPattern {
    public static void main(String[] args) throws InterruptedException {
        // Create the mediator with time limit (10 seconds)
        TimeLimitedAuctionHouse auctionHouse = new TimeLimitedAuctionHouse("Vintage Painting", 10);

        // Create bidders (Colleagues)
        Participant bidder1 = new Participant("Alice", 5000);
        Participant bidder2 = new Participant("Bob", 8000);
        Participant bidder3 = new Participant("Charlie", 3000);

        // Register bidders
        System.out.println("=== REGISTERING BIDDERS ===");
        auctionHouse.registerBidder(bidder1);
        auctionHouse.registerBidder(bidder2);
        auctionHouse.registerBidder(bidder3);

        // Start bidding
        System.out.println("\n=== AUCTION STARTS (10 second limit) ===");
        bidder1.placeBid(1000);

        System.out.println();
        bidder2.placeBid(2000);

        System.out.println();
        bidder3.placeBid(1500);

        System.out.println();
        bidder1.placeBid(3000);

        System.out.println();
        bidder2.placeBid(4000);

        // Simulate waiting for auction to expire
        System.out.println("\n=== WAITING FOR AUCTION TIME LIMIT ===");
        System.out.println("Waiting 11 seconds for auction to expire...");
        Thread.sleep(11000);

        // Try to place bid after time limit
        System.out.println("\n=== ATTEMPTING BID AFTER TIME LIMIT ===");
        bidder3.placeBid(5000);

        // Announce results and show logs
        System.out.println();
        auctionHouse.announceHighestBid();
        auctionHouse.printBidLog();
    }
}

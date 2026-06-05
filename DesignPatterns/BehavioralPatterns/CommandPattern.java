package DesignPatterns.BehavioralPatterns;

import java.util.Stack;

// Receiver: TV
class Television {
    private boolean isOn = false;
    private int volume = 50;
    private int channel = 1;

    public void turnOn() {
        isOn = true;
        System.out.println("TV turned ON");
    }

    public void turnOff() {
        isOn = false;
        System.out.println("TV turned OFF");
    }

    public void volumeUp() {
        if (isOn && volume < 100) {
            volume++;
            System.out.println("Volume increased to: " + volume);
        }
    }

    public void volumeDown() {
        if (isOn && volume > 0) {
            volume--;
            System.out.println("Volume decreased to: " + volume);
        }
    }

    public void changeChannel(int newChannel) {
        if (isOn) {
            channel = newChannel;
            System.out.println("Channel changed to: " + channel);
        }
    }

    public void displayStatus() {
        System.out.println(
                "TV Status - Power: " + (isOn ? "ON" : "OFF") + ", Volume: " + volume + ", Channel: " + channel);
    }
}

// Command interface
interface Command {
    void execute();

    void undo();
}

// Concrete Commands
class TurnOnCommand implements Command {
    private Television tv;

    public TurnOnCommand(Television tv) {
        this.tv = tv;
    }

    @Override
    public void execute() {
        tv.turnOn();
    }

    @Override
    public void undo() {
        tv.turnOff();
    }
}

class TurnOffCommand implements Command {
    private Television tv;

    public TurnOffCommand(Television tv) {
        this.tv = tv;
    }

    @Override
    public void execute() {
        tv.turnOff();
    }

    @Override
    public void undo() {
        tv.turnOn();
    }
}

class VolumeUpCommand implements Command {
    private Television tv;

    public VolumeUpCommand(Television tv) {
        this.tv = tv;
    }

    @Override
    public void execute() {
        tv.volumeUp();
    }

    @Override
    public void undo() {
        tv.volumeDown();
    }
}

class VolumeDownCommand implements Command {
    private Television tv;

    public VolumeDownCommand(Television tv) {
        this.tv = tv;
    }

    @Override
    public void execute() {
        tv.volumeDown();
    }

    @Override
    public void undo() {
        tv.volumeUp();
    }
}

class ChangeChannelCommand implements Command {
    private Television tv;
    private int newChannel;
    private int previousChannel;

    public ChangeChannelCommand(Television tv, int channel) {
        this.tv = tv;
        this.newChannel = channel;
        this.previousChannel = 1;
    }

    @Override
    public void execute() {
        previousChannel = 1; // Store previous channel
        tv.changeChannel(newChannel);
    }

    @Override
    public void undo() {
        tv.changeChannel(previousChannel);
    }
}

// Invoker: RemoteControl
class RemoteControl {
    private Command command;
    private Stack<Command> commandHistory = new Stack<>();

    public void setCommand(Command command) {
        this.command = command;
    }

    public void pressButton() {
        if (command != null) {
            command.execute();
            commandHistory.push(command);
        }
    }

    public void undoLastCommand() {
        if (!commandHistory.isEmpty()) {
            Command lastCommand = commandHistory.pop();
            lastCommand.undo();
            System.out.println("Undo executed");
        } else {
            System.out.println("No commands to undo");
        }
    }

    public void showCommandHistory() {
        System.out.println("Command History: " + commandHistory.size() + " commands executed");
    }
}

// Demo class
public class CommandPattern {
    public static void main(String[] args) {
        // Create receiver
        Television tv = new Television();

        // Create invoker
        RemoteControl remote = new RemoteControl();

        System.out.println("=== Turning TV ON ===");
        remote.setCommand(new TurnOnCommand(tv));
        remote.pressButton();

        System.out.println("\n=== Adjusting Volume ===");
        remote.setCommand(new VolumeUpCommand(tv));
        remote.pressButton();
        remote.pressButton();
        remote.pressButton();

        System.out.println("\n=== Changing Channel ===");
        remote.setCommand(new ChangeChannelCommand(tv, 42));
        remote.pressButton();

        System.out.println("\n=== More Volume Up ===");
        remote.setCommand(new VolumeUpCommand(tv));
        remote.pressButton();
        remote.pressButton();

        System.out.println("\n=== Volume Down ===");
        remote.setCommand(new VolumeDownCommand(tv));
        remote.pressButton();

        System.out.println("\n=== Command History ===");
        remote.showCommandHistory();

        System.out.println("\n=== Current TV Status ===");
        tv.displayStatus();

        System.out.println("\n=== Undo Operations ===");
        remote.undoLastCommand();
        remote.undoLastCommand();
        remote.undoLastCommand();

        System.out.println("\n=== TV Status After Undo ===");
        tv.displayStatus();

        System.out.println("\n=== Turning TV OFF ===");
        remote.setCommand(new TurnOffCommand(tv));
        remote.pressButton();
    }
}

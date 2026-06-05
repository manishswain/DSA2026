package DesignPatterns.BehavioralPatterns;

// Request class for Leave Approval
class LeaveRequest {
    private String employeeId;
    private String employeeName;
    private String leaveType;
    private int numberOfDays;
    private String reason;

    public LeaveRequest(String employeeId, String employeeName, String leaveType, int numberOfDays, String reason) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.leaveType = leaveType;
        this.numberOfDays = numberOfDays;
        this.reason = reason;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getLeaveType() {
        return leaveType;
    }

    public int getNumberOfDays() {
        return numberOfDays;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "LeaveRequest{" +
                "employeeId='" + employeeId + '\'' +
                ", employeeName='" + employeeName + '\'' +
                ", leaveType='" + leaveType + '\'' +
                ", numberOfDays=" + numberOfDays +
                ", reason='" + reason + '\'' +
                '}';
    }
}

// Abstract Handler
abstract class LeaveApprover {
    protected LeaveApprover nextApprover;
    protected String name;
    protected int approvalLimit; // Maximum days that can be approved

    public LeaveApprover(String name, int approvalLimit) {
        this.name = name;
        this.approvalLimit = approvalLimit;
    }

    public void setNextApprover(LeaveApprover nextApprover) {
        this.nextApprover = nextApprover;
    }

    public void processLeaveRequest(LeaveRequest request) {
        if (request.getNumberOfDays() <= approvalLimit) {
            approveLeave(request);
        } else if (nextApprover != null) {
            System.out.println(name + " cannot approve " + request.getNumberOfDays() +
                    " days leave (Limit: " + approvalLimit + " days). Forwarding to next level...\n");
            nextApprover.processLeaveRequest(request);
        } else {
            rejectLeave(request);
        }
    }

    protected void approveLeave(LeaveRequest request) {
        System.out.println("✓ APPROVED by " + name);
        System.out.println("  Employee: " + request.getEmployeeName() + " (" + request.getEmployeeId() + ")");
        System.out.println("  Leave Type: " + request.getLeaveType());
        System.out.println("  Duration: " + request.getNumberOfDays() + " days");
        System.out.println("  Reason: " + request.getReason());
        System.out.println("  Approved Limit: " + approvalLimit + " days");
    }

    protected void rejectLeave(LeaveRequest request) {
        System.out.println("✗ REJECTED - Leave duration (" + request.getNumberOfDays() +
                " days) exceeds all approval limits");
    }

    public String getName() {
        return name;
    }

    public int getApprovalLimit() {
        return approvalLimit;
    }
}

// Concrete Handler: Supervisor
class Supervisor extends LeaveApprover {
    public Supervisor() {
        super("Supervisor", 3); // Can approve up to 3 days
    }
}

// Concrete Handler: Manager
class Manager extends LeaveApprover {
    public Manager() {
        super("Manager", 7); // Can approve up to 7 days
    }
}

// Concrete Handler: Director
class Director extends LeaveApprover {
    public Director() {
        super("Director", 30); // Can approve up to 30 days
    }
}

// Demo class
public class ChainOfResponsibility {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("CHAIN OF RESPONSIBILITY - LEAVE APPROVAL");
        System.out.println("========================================\n");

        // Create the chain of handlers
        LeaveApprover supervisor = new Supervisor();
        LeaveApprover manager = new Manager();
        LeaveApprover director = new Director();

        // Set up the chain: Supervisor -> Manager -> Director
        supervisor.setNextApprover(manager);
        manager.setNextApprover(director);

        System.out.println("Chain Setup: Supervisor (3 days) -> Manager (7 days) -> Director (30 days)\n");

        // Test Case 1: Leave within Supervisor's limit
        System.out.println("--- Test Case 1: 2 Days Leave (Supervisor's Limit) ---");
        LeaveRequest request1 = new LeaveRequest("EMP-001", "John Smith", "Sick Leave", 2, "Medical checkup");
        System.out.println("Processing: " + request1);
        supervisor.processLeaveRequest(request1);

        // Test Case 2: Leave requires Manager approval
        System.out.println("\n--- Test Case 2: 5 Days Leave (Manager's Limit) ---");
        LeaveRequest request2 = new LeaveRequest("EMP-002", "Sarah Johnson", "Casual Leave", 5, "Family vacation");
        System.out.println("Processing: " + request2);
        supervisor.processLeaveRequest(request2);

        // Test Case 3: Leave requires Director approval
        System.out.println("\n--- Test Case 3: 15 Days Leave (Director's Limit) ---");
        LeaveRequest request3 = new LeaveRequest("EMP-003", "Mike Chen", "Annual Leave", 15, "Holiday trip abroad");
        System.out.println("Processing: " + request3);
        supervisor.processLeaveRequest(request3);

        // Test Case 4: Leave exceeds all limits
        System.out.println("\n--- Test Case 4: 45 Days Leave (Exceeds All Limits) ---");
        LeaveRequest request4 = new LeaveRequest("EMP-004", "Emily Davis", "Sabbatical", 45, "Personal development");
        System.out.println("Processing: " + request4);
        supervisor.processLeaveRequest(request4);

        // Test Case 5: Leave at Manager level
        System.out.println("\n--- Test Case 5: 3 Days Leave (Start at Manager Level) ---");
        LeaveRequest request5 = new LeaveRequest("EMP-005", "Alex Wilson", "Sick Leave", 3, "Medical emergency");
        System.out.println("Processing: " + request5);
        manager.processLeaveRequest(request5);

        // Test Case 6: Long leave at Director level
        System.out.println("\n--- Test Case 6: 20 Days Leave (Director Only) ---");
        LeaveRequest request6 = new LeaveRequest("EMP-006", "Lisa Anderson", "Annual Leave", 20,
                "Extended family event");
        System.out.println("Processing: " + request6);
        director.processLeaveRequest(request6);

        System.out.println("\n========================================");
        System.out.println("DEMO COMPLETE");
        System.out.println("========================================");
    }
}

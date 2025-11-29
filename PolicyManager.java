import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * PolicyManager.java
 *
 * Single-file console application for managing PolicyHolder records.
 * - Contains nested PolicyHolder class
 * - Menu driven: add, list, maturity calc, findHighValuePolicies, exit
 *
 * Compile:
 *   javac PolicyManager.java
 *
 * Run:
 *   java PolicyManager
 *
 * (Requires Java 8+)
 */
public class PolicyManager {

    // Fixed annual return used for maturity calculation (8%).
    private static final double ANNUAL_RETURN = 0.08;
    private static final double HIGH_VALUE_THRESHOLD = 100000.0;

    // In-memory store for policies (use LinkedHashMap to preserve insertion order)
    private final Map<String, PolicyHolder> policyMap = new LinkedHashMap<>();
    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        PolicyManager app = new PolicyManager();
        app.seedSampleData(); // optional - remove if you want empty start
        app.run();
    }

    /** Main interactive loop */
    private void run() {
        printHeader();
        boolean running = true;
        while (running) {
            printMenu();
            String choice = prompt("Enter choice");
            switch (choice.trim()) {
                case "1":
                    addPolicyInteractive();
                    break;
                case "2":
                    listPolicies();
                    break;
                case "3":
                    calculateMaturityInteractive();
                    break;
                case "4":
                    findHighValuePolicies(new ArrayList<>(policyMap.values()));
                    break;
                case "5":
                    removePolicyInteractive();
                    break;
                case "6":
                    System.out.println("Exiting. Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }
        scanner.close();
    }

    /* -------------------------
       UI / Helpers
       ------------------------- */

    private void printHeader() {
        System.out.println("=======================================");
        System.out.println("   Policy Holder Management Console");
        System.out.println("=======================================");
    }

    private void printMenu() {
        System.out.println("\nMenu:");
        System.out.println("  1) Add a Policy Holder");
        System.out.println("  2) List all Policy Holders");
        System.out.println("  3) Calculate maturity value for a policy");
        System.out.println("  4) Find high-value policies (investmentAmount > 100,000)");
        System.out.println("  5) Remove a policy by policyId");
        System.out.println("  6) Exit");
    }

    private String prompt(String label) {
        System.out.print(label + ": ");
        return scanner.nextLine();
    }

    private void pause() {
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }

    /* -------------------------
       CRUD-like operations
       ------------------------- */

    private void addPolicyInteractive() {
        System.out.println("\nAdd Policy Holder (enter values). Leave policyId blank to auto-generate.");
        String policyId = prompt("policyId");
        if (policyId == null || policyId.trim().isEmpty()) {
            policyId = UUID.randomUUID().toString();
            System.out.println("Generated policyId -> " + policyId);
        } else {
            if (policyMap.containsKey(policyId)) {
                System.out.println("A policy with this ID already exists. Aborting add.");
                return;
            }
        }

        String name = prompt("name");
        if (name == null || name.trim().isEmpty()) {
            System.out.println("Name cannot be empty. Aborting.");
            return;
        }

        Double investmentAmount = readDouble("investmentAmount (e.g., 150000)");
        if (investmentAmount == null || investmentAmount < 0) {
            System.out.println("Invalid investment amount. Aborting.");
            return;
        }

        Integer yearsInForce = readInt("yearsInForce (integer)");
        if (yearsInForce == null || yearsInForce < 0) {
            System.out.println("Invalid yearsInForce. Aborting.");
            return;
        }

        PolicyHolder ph = new PolicyHolder(policyId, name.trim(), investmentAmount, yearsInForce);
        policyMap.put(policyId, ph);
        System.out.println("Policy added successfully:");
        System.out.println(ph);
    }

    private void listPolicies() {
        System.out.println("\nStored Policy Holders (" + policyMap.size() + "):");
        if (policyMap.isEmpty()) {
            System.out.println("  (none)");
        } else {
            NumberFormat nf = NumberFormat.getCurrencyInstance();
            for (PolicyHolder ph : policyMap.values()) {
                System.out.println("-------------------------------------------------");
                System.out.println("policyId       : " + ph.getPolicyId());
                System.out.println("name           : " + ph.getName());
                System.out.println("investmentAmt  : " + nf.format(ph.getInvestmentAmount()));
                System.out.println("yearsInForce   : " + ph.getYearsInForce());
            }
            System.out.println("-------------------------------------------------");
        }
        pause();
    }

    private void calculateMaturityInteractive() {
        String policyId = prompt("\nEnter policyId to calculate maturity for (or leave blank to provide custom values)");
        double principal;
        int years;

        if (policyId == null || policyId.trim().isEmpty()) {
            Double p = readDouble("Investment amount");
            if (p == null || p < 0) {
                System.out.println("Invalid investment amount. Aborting.");
                return;
            }
            Integer y = readInt("Years in force");
            if (y == null || y < 0) {
                System.out.println("Invalid years. Aborting.");
                return;
            }
            principal = p;
            years = y;
        } else {
            PolicyHolder ph = policyMap.get(policyId);
            if (ph == null) {
                System.out.println("No policy found with id: " + policyId);
                return;
            }
            principal = ph.getInvestmentAmount();
            years = ph.getYearsInForce();
            System.out.println("Using values from policy: " + ph.getPolicyId() + " (" + ph.getName() + ")");
        }

        double futureValue = calculateMaturityValue(principal, years);
        NumberFormat nf = NumberFormat.getCurrencyInstance();
        System.out.println("\nMaturity Calculation:");
        System.out.println("  Principal: " + nf.format(principal));
        System.out.println("  Years    : " + years);
        System.out.println("  Annual Return Assumed: 8% (fixed)");
        System.out.println("  Future Value: " + nf.format(futureValue));
        pause();
    }

    private void removePolicyInteractive() {
        String policyId = prompt("\nEnter policyId to remove");
        if (policyId == null || policyId.trim().isEmpty()) {
            System.out.println("policyId required.");
            return;
        }
        PolicyHolder removed = policyMap.remove(policyId);
        if (removed == null) {
            System.out.println("No policy with id: " + policyId);
        } else {
            System.out.println("Removed policy: " + removed.getPolicyId() + " (" + removed.getName() + ")");
        }
    }

    /* -------------------------
       Required static methods
       ------------------------- */

    /**
     * Calculate future value using fixed annual return (8%).
     * Formula: Future Value = Investment Amount * (1 + r) ^ years
     *
     * @param investmentAmount principal (must be >= 0)
     * @param yearsInForce number of years (must be >= 0)
     * @return future value as double
     */
    public static double calculateMaturityValue(double investmentAmount, int yearsInForce) {
        if (investmentAmount < 0 || yearsInForce < 0) {
            throw new IllegalArgumentException("investmentAmount and yearsInForce must be non-negative");
        }
        // Compute (1 + r) ^ n carefully using Math.pow
        double factor = Math.pow(1.0 + ANNUAL_RETURN, yearsInForce);
        return investmentAmount * factor;
    }

    /**
     * Iterate through listPolicyHolders and print details for holders with
     * investmentAmount > 100,000.
     *
     * @param list list of PolicyHolder objects
     */
    public static void findHighValuePolicies(List<PolicyHolder> list) {
        if (list == null || list.isEmpty()) {
            System.out.println("\nNo policy holders available.");
            return;
        }
        System.out.println("\nHigh-value policy holders (investmentAmount > 100,000):");
        NumberFormat nf = NumberFormat.getCurrencyInstance();
        List<PolicyHolder> found = list.stream()
                .filter(p -> p.getInvestmentAmount() != null && p.getInvestmentAmount() > HIGH_VALUE_THRESHOLD)
                .collect(Collectors.toList());
        if (found.isEmpty()) {
            System.out.println("  (none)");
        } else {
            for (PolicyHolder p : found) {
                System.out.println("-------------------------------------------------");
                System.out.println("policyId      : " + p.getPolicyId());
                System.out.println("name          : " + p.getName());
                System.out.println("investmentAmt : " + nf.format(p.getInvestmentAmount()));
                System.out.println("yearsInForce  : " + p.getYearsInForce());
            }
            System.out.println("-------------------------------------------------");
        }
    }

    /* -------------------------
       Utilities & sample seed
       ------------------------- */

    private Double readDouble(String label) {
        String s = prompt(label);
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception ex) {
            System.out.println("Invalid number: " + s);
            return null;
        }
    }

    private Integer readInt(String label) {
        String s = prompt(label);
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception ex) {
            System.out.println("Invalid integer: " + s);
            return null;
        }
    }

    /** Optional: add a couple of sample policies to demonstrate features quickly */
    private void seedSampleData() {
        PolicyHolder p1 = new PolicyHolder("PH-001", "ashhar kaunain khan", 120000.0, 5);
        PolicyHolder p2 = new PolicyHolder("PH-002", "rahul kumar", 75000.0, 3);
        PolicyHolder p3 = new PolicyHolder("PH-003", "ajay Kumar", 200000.0, 10);
        policyMap.put(p1.getPolicyId(), p1);
        policyMap.put(p2.getPolicyId(), p2);
        policyMap.put(p3.getPolicyId(), p3);
    }

    /* -------------------------
       PolicyHolder Class (POJO)
       ------------------------- */
    public static class PolicyHolder {
        private String policyId;
        private String name;
        private Double investmentAmount;
        private Integer yearsInForce;

        public PolicyHolder() {}

        public PolicyHolder(String policyId, String name, Double investmentAmount, Integer yearsInForce) {
            this.policyId = policyId;
            this.name = name;
            this.investmentAmount = investmentAmount;
            this.yearsInForce = yearsInForce;
        }

        public String getPolicyId() {
            return policyId;
        }

        public void setPolicyId(String policyId) {
            this.policyId = policyId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Double getInvestmentAmount() {
            return investmentAmount;
        }

        public void setInvestmentAmount(Double investmentAmount) {
            this.investmentAmount = investmentAmount;
        }

        public Integer getYearsInForce() {
            return yearsInForce;
        }

        public void setYearsInForce(Integer yearsInForce) {
            this.yearsInForce = yearsInForce;
        }

        @Override
        public String toString() {
            NumberFormat nf = NumberFormat.getCurrencyInstance();
            return "PolicyHolder{" +
                    "policyId='" + policyId + '\'' +
                    ", name='" + name + '\'' +
                    ", investmentAmount=" + (investmentAmount == null ? "null" : nf.format(investmentAmount)) +
                    ", yearsInForce=" + yearsInForce +
                    '}';
        }
    }
}

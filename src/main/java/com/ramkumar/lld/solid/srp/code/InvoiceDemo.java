package com.ramkumar.lld.solid.srp.code;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Scenario A: Invoice Processing System
 *
 * Demonstrates the Single Responsibility Principle (SRP).
 *
 * STEP 1 — shows the VIOLATION: InvoiceProcessorGod is one class with five
 *           different reasons to change (five different actors who demand changes).
 *
 * STEP 2 — shows the FIX: five focused classes, each with one responsibility,
 *           orchestrated by InvoiceService.
 *
 * Key SRP concepts illustrated:
 *   - "Reason to change" = the stakeholder / actor who demands the change
 *   - God class smell: name ends in "Manager/Processor", long import list,
 *     method groups that serve different teams
 *   - Extract Class refactoring move
 *   - High cohesion vs low cohesion
 */
public class InvoiceDemo {

    // =========================================================================
    // DOMAIN MODEL — shared across both the violation and the fix
    // =========================================================================

    // ENCAPSULATION — all fields private final; only getters exposed
    static class InvoiceItem {
        private final String name;
        private final double unitPrice;
        private final int    quantity;

        public InvoiceItem(String name, double unitPrice, int quantity) {
            if (name == null || name.isBlank())
                throw new IllegalArgumentException("Item name cannot be blank");
            if (unitPrice < 0) throw new IllegalArgumentException("Unit price cannot be negative");
            if (quantity  < 1) throw new IllegalArgumentException("Quantity must be at least 1");
            this.name      = name;
            this.unitPrice = unitPrice;
            this.quantity  = quantity;
        }

        public String getName()       { return name; }
        public double getUnitPrice()  { return unitPrice; }
        public int    getQuantity()   { return quantity; }
        public double getLineTotal()  { return unitPrice * quantity; }   // convenience — belongs with item data
    }

    // =========================================================================
    // ❌  VIOLATION — InvoiceProcessorGod (one class, five reasons to change)
    // =========================================================================

    /**
     * GOD CLASS — this is the WRONG design. Do not write code like this.
     *
     * Five different teams can demand a change:
     *   Reason 1 (Data team)    → fields: invoiceId, customer, items
     *   Reason 2 (Finance team) → calculateTotal(), applyTax()
     *   Reason 3 (UI/Print)     → toText(), toCsv()
     *   Reason 4 (DBA team)     → save(), findById()
     *   Reason 5 (Ops team)     → sendEmail()
     *
     * Problems:
     *   - Changing tax rules forces you to retest formatting code.
     *   - Changing the DB schema forces you to retest email code.
     *   - Impossible to unit-test tax logic in isolation (requires DB + email mocks).
     *   - Every sprint, this file is touched — merge conflict hotspot.
     */
    @SuppressWarnings("unused")  // intentionally incomplete to show the smell, not run
    static class InvoiceProcessorGod {

        // Reason 1: Data — data modeller changes these
        private final String            invoiceId;
        private final String            customerName;
        private final List<InvoiceItem> items;

        public InvoiceProcessorGod(String invoiceId, String customerName) {
            this.invoiceId    = invoiceId;
            this.customerName = customerName;
            this.items        = new ArrayList<>();
        }

        public void addItem(InvoiceItem item) { items.add(item); }

        // Reason 2: Finance — tax rules change when government policy changes
        public double calculateSubtotal() {
            return items.stream().mapToDouble(InvoiceItem::getLineTotal).sum();
        }
        public double applyTax(double subtotal, double taxRate) {
            return subtotal + subtotal * taxRate;
        }

        // Reason 3: UI / Print team — format changes when stakeholder wants PDF or HTML
        public String toText() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== INVOICE ").append(invoiceId).append(" ===\n");
            sb.append("Customer: ").append(customerName).append("\n");
            for (InvoiceItem i : items)
                sb.append(i.getName()).append(" x").append(i.getQuantity())
                        .append(" = ₹").append(i.getLineTotal()).append("\n");
            sb.append("Total: ₹").append(calculateSubtotal());
            return sb.toString();
        }
        public String toCsv() {
            StringBuilder sb = new StringBuilder("item,qty,price\n");
            for (InvoiceItem i : items)
                sb.append(i.getName()).append(",").append(i.getQuantity())
                        .append(",").append(i.getLineTotal()).append("\n");
            return sb.toString();
        }

        // Reason 4: DBA — DB schema or storage mechanism changes
        public void save() {
            // Simulated: INSERT INTO invoices (id, customer) VALUES (...)
            System.out.println("[DB] Saved invoice " + invoiceId);
        }
        public void findById(String id) {
            System.out.println("[DB] SELECT * FROM invoices WHERE id = " + id);
        }

        // Reason 5: Ops / Comms team — email provider or template changes
        public void sendEmail(String toAddress) {
            System.out.println("[EMAIL] Sending invoice " + invoiceId + " to " + toAddress);
        }
    }

    // =========================================================================
    // ✅  FIX — Six focused classes, each with ONE reason to change
    // =========================================================================

    // ── 1. Invoice — DATA ONLY ────────────────────────────────────────────────
    // Reason to change: Data modeller changes field names or adds a new field.
    // SRP responsibility: "Holds invoice data."
    static class Invoice {

        private static int counter = 0;

        private final String            invoiceId;
        private final String            customerName;
        private final String            customerEmail;
        private final List<InvoiceItem> items;

        public Invoice(String customerName, String customerEmail) {
            if (customerName  == null || customerName.isBlank())
                throw new IllegalArgumentException("Customer name cannot be blank");
            if (customerEmail == null || customerEmail.isBlank())
                throw new IllegalArgumentException("Customer email cannot be blank");
            this.invoiceId     = String.format("INV-%03d", ++counter);
            this.customerName  = customerName;
            this.customerEmail = customerEmail;
            this.items         = new ArrayList<>();
        }

        public void addItem(InvoiceItem item) {
            if (item == null) throw new IllegalArgumentException("Item cannot be null");
            items.add(item);
        }

        public String            getInvoiceId()     { return invoiceId; }
        public String            getCustomerName()  { return customerName; }
        public String            getCustomerEmail() { return customerEmail; }
        public List<InvoiceItem> getItems()         { return Collections.unmodifiableList(items); }
    }

    // ── 2. TaxCalculator — BUSINESS LOGIC ONLY ───────────────────────────────
    // Reason to change: Finance department changes tax rules (GST rate, exemptions…).
    // SRP responsibility: "Calculates tax and totals for an invoice."
    static class TaxCalculator {

        private static final double DEFAULT_TAX_RATE = 0.18;   // 18 % GST

        // HIGH COHESION — every method in this class is about tax calculation
        public double calculateSubtotal(Invoice invoice) {
            return invoice.getItems().stream()
                          .mapToDouble(InvoiceItem::getLineTotal)
                          .sum();
        }

        public double calculateTax(Invoice invoice) {
            return calculateSubtotal(invoice) * DEFAULT_TAX_RATE;
        }

        public double calculateTotal(Invoice invoice) {
            double subtotal = calculateSubtotal(invoice);
            return subtotal + subtotal * DEFAULT_TAX_RATE;
        }
    }

    // ── 3. InvoiceFormatter — FORMATTING ONLY ────────────────────────────────
    // Reason to change: UI/Print team changes output format (add HTML, change layout).
    // SRP responsibility: "Converts an invoice into a human-readable format."
    static class InvoiceFormatter {

        // Depends on both Invoice (data) and TaxCalculator (totals) — both injected
        private final TaxCalculator taxCalculator;

        public InvoiceFormatter(TaxCalculator taxCalculator) {
            this.taxCalculator = taxCalculator;
        }

        public String toText(Invoice invoice) {
            StringBuilder sb = new StringBuilder();
            sb.append("=== INVOICE ").append(invoice.getInvoiceId()).append(" ===\n");
            sb.append("Customer : ").append(invoice.getCustomerName()).append("\n");
            sb.append("─────────────────────────────────────\n");
            for (InvoiceItem item : invoice.getItems())
                sb.append(String.format("  %-20s x%d  ₹%.2f%n",
                        item.getName(), item.getQuantity(), item.getLineTotal()));
            sb.append("─────────────────────────────────────\n");
            sb.append(String.format("Subtotal : ₹%.2f%n", taxCalculator.calculateSubtotal(invoice)));
            sb.append(String.format("Tax(18%%) : ₹%.2f%n", taxCalculator.calculateTax(invoice)));
            sb.append(String.format("Total    : ₹%.2f%n", taxCalculator.calculateTotal(invoice)));
            return sb.toString();
        }

        public String toCsv(Invoice invoice) {
            StringBuilder sb = new StringBuilder("item,qty,unitPrice,lineTotal\n");
            for (InvoiceItem item : invoice.getItems())
                sb.append(item.getName()).append(",")
                  .append(item.getQuantity()).append(",")
                  .append(item.getUnitPrice()).append(",")
                  .append(item.getLineTotal()).append("\n");
            return sb.toString();
        }
    }

    // ── 4. InvoiceRepository — PERSISTENCE ONLY ──────────────────────────────
    // Reason to change: DBA changes the schema, switches from SQL to NoSQL.
    // SRP responsibility: "Stores and retrieves invoices."
    static class InvoiceRepository {

        // In-memory store; real impl would use JDBC / JPA
        private final List<Invoice> store = new ArrayList<>();

        public void save(Invoice invoice) {
            store.add(invoice);
            System.out.println("[REPO] Saved " + invoice.getInvoiceId());
        }

        public Invoice findById(String invoiceId) {
            return store.stream()
                        .filter(i -> i.getInvoiceId().equals(invoiceId))
                        .findFirst()
                        .orElse(null);
        }

        public List<Invoice> findAll() {
            return Collections.unmodifiableList(store);
        }
    }

    // ── 5. InvoiceNotifier — NOTIFICATION ONLY ───────────────────────────────
    // Reason to change: Ops team switches email provider, adds SMS channel.
    // SRP responsibility: "Sends notifications about an invoice."
    static class InvoiceNotifier {

        public void sendEmail(Invoice invoice, String formattedText) {
            System.out.println("[EMAIL] To: " + invoice.getCustomerEmail());
            System.out.println("[EMAIL] Subject: Invoice " + invoice.getInvoiceId() + " from ShopCo");
            System.out.println("[EMAIL] Body preview: " + formattedText.lines().findFirst().orElse(""));
        }
    }

    // ── 6. InvoiceService — ORCHESTRATION ONLY ───────────────────────────────
    // Reason to change: Workflow changes (e.g., "also save a PDF after sending email").
    // SRP responsibility: "Coordinates the invoice workflow."
    // LOW COUPLING — InvoiceService depends on abstractions; each collaborator is injected
    static class InvoiceService {

        private final TaxCalculator      taxCalculator;
        private final InvoiceFormatter   formatter;
        private final InvoiceRepository  repository;
        private final InvoiceNotifier    notifier;

        // DEPENDENCY INJECTION — collaborators are passed in, not created here
        public InvoiceService(TaxCalculator taxCalculator,
                              InvoiceFormatter formatter,
                              InvoiceRepository repository,
                              InvoiceNotifier notifier) {
            this.taxCalculator = taxCalculator;
            this.formatter     = formatter;
            this.repository    = repository;
            this.notifier      = notifier;
        }

        // The workflow in ONE place — easy to read, easy to change independently
        public void processAndSend(Invoice invoice) {
            String text = formatter.toText(invoice);
            repository.save(invoice);
            notifier.sendEmail(invoice, text);
            System.out.println("[SERVICE] Done. Total = ₹"
                    + String.format("%.2f", taxCalculator.calculateTotal(invoice)));
        }
    }

    // =========================================================================
    // Main — demonstrates the SRP-compliant design end to end
    // =========================================================================

    public static void main(String[] args) {

        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println(" SRP Demo: Invoice Processing System");
        System.out.println("═══════════════════════════════════════════════════════\n");

        // Build the invoice (data only)
        Invoice invoice = new Invoice("Alice Johnson", "alice@example.com");
        invoice.addItem(new InvoiceItem("Laptop",    75000.00, 1));
        invoice.addItem(new InvoiceItem("Mouse",      1500.00, 2));
        invoice.addItem(new InvoiceItem("USB Hub",    2000.00, 1));

        System.out.println("── 1. Tax calculation (Finance team logic) ──────────────");
        TaxCalculator tax = new TaxCalculator();
        System.out.printf("Subtotal : ₹%.2f%n", tax.calculateSubtotal(invoice));
        System.out.printf("Tax(18%%) : ₹%.2f%n", tax.calculateTax(invoice));
        System.out.printf("Total    : ₹%.2f%n",  tax.calculateTotal(invoice));

        System.out.println("\n── 2. Formatting (UI/Print team logic) ─────────────────");
        InvoiceFormatter formatter = new InvoiceFormatter(tax);
        System.out.println(formatter.toText(invoice));

        System.out.println("── 3. CSV output (different format, same formatter class)─");
        System.out.println(formatter.toCsv(invoice));

        System.out.println("── 4. Persistence (DBA logic) ───────────────────────────");
        InvoiceRepository repo = new InvoiceRepository();
        repo.save(invoice);
        Invoice found = repo.findById(invoice.getInvoiceId());
        System.out.println("Found: " + (found != null ? found.getInvoiceId() : "NOT FOUND"));

        System.out.println("\n── 5. Full workflow via InvoiceService ──────────────────");
        InvoiceNotifier notifier = new InvoiceNotifier();
        InvoiceService  service  = new InvoiceService(tax, formatter, repo, notifier);

        Invoice invoice2 = new Invoice("Bob Smith", "bob@example.com");
        invoice2.addItem(new InvoiceItem("Keyboard", 3500.00, 1));
        invoice2.addItem(new InvoiceItem("Monitor",  18000.00, 2));
        service.processAndSend(invoice2);

        System.out.println("\n── 6. SRP proof: changing tax rate only touches TaxCalculator");
        System.out.println("── Changing email template only touches InvoiceNotifier");
        System.out.println("── Changing DB schema only touches InvoiceRepository");
        System.out.println("── Each class has exactly one reason to change.");
    }
}

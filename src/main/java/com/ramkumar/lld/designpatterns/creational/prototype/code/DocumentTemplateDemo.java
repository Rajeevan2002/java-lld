package com.ramkumar.lld.designpatterns.creational.prototype.code;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Scenario A — Document Template Cloning
 *
 * Demonstrates the Prototype pattern using document templates.
 * A base document (Resume, Report, Letter) is built once with expensive setup,
 * then cloned to produce variants. Mutating a clone must not affect the original.
 *
 * ── Participants ─────────────────────────────────────────────────────────────
 *   AbstractPrototype  →  Document       (abstract; declares clone())
 *   ConcretePrototypes →  Resume, Report, Letter
 *   Client             →  main()         (calls clone(), mutates clone, leaves original intact)
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class DocumentTemplateDemo {

    // =========================================================================
    // ABSTRACT PROTOTYPE
    // =========================================================================

    // [AbstractPrototype] — declares the clone() contract
    abstract static class Document {

        // [Mutable fields] — can change after clone
        protected String       title;
        protected String       author;
        // [Deep-copy field] — a List is mutable; MUST be deep-copied in every clone
        protected List<String> sections;

        // [Normal constructor] — used to build the original template
        Document(String title, String author) {
            this.title    = title;
            this.author   = author;
            this.sections = new ArrayList<>();
        }

        // [Copy constructor] — protected; called by subclass copy constructors
        // This is the mechanism that implements the Prototype pattern in Java
        protected Document(Document source) {
            this.title    = source.title;    // String — immutable, safe to share
            this.author   = source.author;   // String — immutable, safe to share
            // [Deep copy] — new ArrayList so clone's list ≠ original's list
            this.sections = new ArrayList<>(source.sections);
        }

        // [Abstract clone()] — each ConcretePrototype provides its own implementation
        public abstract Document clone();

        // Mutators — work on the instance (whether original or clone)
        public void addSection(String section) {
            if (section == null || section.isBlank())
                throw new IllegalArgumentException("section must not be blank");
            sections.add(section);
        }

        public void removeSection(String section) {
            if (!sections.remove(section))
                throw new NoSuchElementException("section not found: " + section);
        }

        // [Encapsulation] — returns unmodifiable view; caller cannot mutate internal list
        public List<String> getSections() {
            return Collections.unmodifiableList(sections);
        }

        public String getTitle()  { return title; }
        public String getAuthor() { return author; }
        public void setTitle(String title)   { this.title  = title; }
        public void setAuthor(String author) { this.author = author; }
    }

    // =========================================================================
    // CONCRETE PROTOTYPES
    // =========================================================================

    // [ConcretePrototype A]
    static class Resume extends Document {
        private String targetRole;   // subclass-specific mutable field

        Resume(String title, String author, String targetRole) {
            super(title, author);
            this.targetRole = targetRole;
        }

        // [Copy constructor] — calls super copy constructor, then copies own fields
        private Resume(Resume source) {
            super(source);                     // copies title, author, sections (deep)
            this.targetRole = source.targetRole; // String — immutable, safe
        }

        // [Covariant return] — return Resume (not Document) so callers avoid cast
        @Override
        public Resume clone() {
            return new Resume(this);           // delegates to copy constructor
        }

        public String getTargetRole() { return targetRole; }
        public void setTargetRole(String r) { this.targetRole = r; }

        @Override
        public String toString() {
            return String.format("Resume{title='%s', author='%s', role='%s', sections=%s}",
                title, author, targetRole, sections);
        }
    }

    // [ConcretePrototype B]
    static class Report extends Document {
        private String department;

        Report(String title, String author, String department) {
            super(title, author);
            this.department = department;
        }

        private Report(Report source) {
            super(source);
            this.department = source.department;
        }

        @Override
        public Report clone() { return new Report(this); }

        public String getDepartment() { return department; }
        public void setDepartment(String d) { this.department = d; }

        @Override
        public String toString() {
            return String.format("Report{title='%s', author='%s', dept='%s', sections=%s}",
                title, author, department, sections);
        }
    }

    // [ConcretePrototype C]
    static class Letter extends Document {
        private String recipient;

        Letter(String title, String author, String recipient) {
            super(title, author);
            this.recipient = recipient;
        }

        private Letter(Letter source) {
            super(source);
            this.recipient = source.recipient;
        }

        @Override
        public Letter clone() { return new Letter(this); }

        public String getRecipient() { return recipient; }
        public void setRecipient(String r) { this.recipient = r; }

        @Override
        public String toString() {
            return String.format("Letter{title='%s', author='%s', recipient='%s', sections=%s}",
                title, author, recipient, sections);
        }
    }

    // =========================================================================
    // DEMO
    // =========================================================================
    public static void main(String[] args) {

        System.out.println("═══ Prototype — Document Template Demo ══════════════════════");

        // [Build original template once — expensive setup done here]
        System.out.println("\n── Build original Resume template ────────────────────────────");
        Resume template = new Resume("Software Engineer Resume", "Alice", "Backend Engineer");
        template.addSection("Summary");
        template.addSection("Experience");
        template.addSection("Education");
        template.addSection("Skills");
        System.out.println("Template: " + template);

        // [Clone] — fast; no re-setup needed
        System.out.println("\n── Clone and customise for a different role ──────────────────");
        Resume clone1 = template.clone();
        clone1.setTitle("Senior Engineer Resume");
        clone1.setTargetRole("Senior Backend Engineer");
        clone1.addSection("Publications");    // mutate clone's sections
        System.out.println("Clone 1: " + clone1);

        // [Deep copy check] — clone mutation must not bleed to original
        System.out.println("\n── Deep copy isolation check ─────────────────────────────────");
        System.out.println("Template sections: " + template.getSections());
        System.out.println("Clone 1 sections:  " + clone1.getSections());
        System.out.println("Same list? " + (template.getSections() == clone1.getSections())); // false

        // [Unmodifiable view check]
        System.out.println("\n── getInventory returns unmodifiable view ─────────────────────");
        try {
            template.getSections().add("Hacked");
            System.out.println("FAIL — should have thrown");
        } catch (UnsupportedOperationException e) {
            System.out.println("PASSED — getSections() returns unmodifiable view");
        }

        // [Polymorphism] — same clone() call works for Report and Letter too
        System.out.println("\n── Clone Report and Letter ───────────────────────────────────");
        Report reportTemplate = new Report("Q4 Sales Report", "Bob", "Sales");
        reportTemplate.addSection("Executive Summary");
        reportTemplate.addSection("Data");
        Report reportClone = reportTemplate.clone();
        reportClone.setDepartment("Marketing");
        System.out.println("Original: " + reportTemplate);
        System.out.println("Clone:    " + reportClone);

        // [Covariant return — no cast needed]
        System.out.println("\n── Covariant return — no cast ────────────────────────────────");
        Letter letter = new Letter("Offer Letter", "HR", "Bob");
        letter.addSection("Introduction");
        Letter letterClone = letter.clone(); // returns Letter, not Document
        letterClone.setRecipient("Charlie");
        System.out.println("Original: " + letter);
        System.out.println("Clone:    " + letterClone);

        // [removeSection validation]
        System.out.println("\n── removeSection — item not found → NoSuchElementException ─────");
        try {
            template.removeSection("Certifications");
        } catch (NoSuchElementException e) {
            System.out.println("Caught NSE: " + e.getMessage());
        }

        System.out.println("\n── Prototype Summary ─────────────────────────────────────────");
        System.out.println("  Template built once; all variants cloned from it");
        System.out.println("  Deep copy: clone.sections ≠ original.sections (different ArrayList)");
        System.out.println("  Covariant return: clone() returns Resume/Report/Letter, not Document");
        System.out.println("  No Cloneable: copy constructor is the Java-idiomatic approach");
    }
}

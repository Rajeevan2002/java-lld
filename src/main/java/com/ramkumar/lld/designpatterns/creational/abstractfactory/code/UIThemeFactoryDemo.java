package com.ramkumar.lld.designpatterns.creational.abstractfactory.code;

/**
 * Scenario A — UI Theme Factory
 *
 * Demonstrates the Abstract Factory pattern using a UI theming system.
 * The same Application code renders a consistent UI regardless of which
 * theme family is active — all components are guaranteed to match.
 *
 * ── Participants ────────────────────────────────────────────────────────────
 *   AbstractProduct  →  Button, TextBox, Dialog
 *   ConcreteProduct  →  LightButton/DarkButton, LightTextBox/DarkTextBox,
 *                       LightDialog/DarkDialog (and HighContrast variants)
 *   AbstractFactory  →  UIThemeFactory
 *   ConcreteFactory  →  LightThemeFactory, DarkThemeFactory, HighContrastFactory
 *   Client           →  Application (never references concrete types)
 * ────────────────────────────────────────────────────────────────────────────
 */
public class UIThemeFactoryDemo {

    // =========================================================================
    // ABSTRACT PRODUCTS — three product types in the family
    // =========================================================================

    // [AbstractProduct A] — every Button must implement this
    interface Button {
        void render();
        String getLabel();
        String getStyle();    // e.g. "bg:#FFF fg:#000"
    }

    // [AbstractProduct B]
    interface TextBox {
        void render();
        String getPlaceholder();
        String getStyle();
    }

    // [AbstractProduct C]
    interface Dialog {
        void show(String message);
        String getTitle();
        String getStyle();
    }

    // =========================================================================
    // ABSTRACT FACTORY — one factory method per product type
    // =========================================================================

    // [AbstractFactory]
    interface UIThemeFactory {
        Button  createButton(String label);
        TextBox createTextBox(String placeholder);
        Dialog  createDialog(String title);
    }

    // =========================================================================
    // CONCRETE PRODUCTS — FAMILY 1: Light Theme
    // =========================================================================

    // [ConcreteProduct — Family 1, Type A]
    static class LightButton implements Button {
        private final String label;
        LightButton(String label) { this.label = label; }

        @Override
        public void render() {
            System.out.printf("  [Light Button] (%s)  style: %s%n", label, getStyle());
        }
        @Override public String getLabel() { return label; }
        @Override public String getStyle() { return "bg:#FFFFFF fg:#1A1A1A border:#CCCCCC"; }
    }

    // [ConcreteProduct — Family 1, Type B]
    static class LightTextBox implements TextBox {
        private final String placeholder;
        LightTextBox(String placeholder) { this.placeholder = placeholder; }

        @Override
        public void render() {
            System.out.printf("  [Light TextBox] (%s)  style: %s%n", placeholder, getStyle());
        }
        @Override public String getPlaceholder() { return placeholder; }
        @Override public String getStyle()        { return "bg:#F9F9F9 fg:#333333 border:#DDDDDD"; }
    }

    // [ConcreteProduct — Family 1, Type C]
    static class LightDialog implements Dialog {
        private final String title;
        LightDialog(String title) { this.title = title; }

        @Override
        public void show(String message) {
            System.out.printf("  [Light Dialog] '%s': %s  style: %s%n", title, message, getStyle());
        }
        @Override public String getTitle() { return title; }
        @Override public String getStyle() { return "bg:#FFFFFF fg:#000000 shadow:rgba(0,0,0,0.1)"; }
    }

    // =========================================================================
    // CONCRETE PRODUCTS — FAMILY 2: Dark Theme
    // =========================================================================

    // [ConcreteProduct — Family 2, Type A]
    static class DarkButton implements Button {
        private final String label;
        DarkButton(String label) { this.label = label; }

        @Override
        public void render() {
            System.out.printf("  [Dark  Button] (%s)  style: %s%n", label, getStyle());
        }
        @Override public String getLabel() { return label; }
        @Override public String getStyle() { return "bg:#1E1E1E fg:#E0E0E0 border:#444444"; }
    }

    // [ConcreteProduct — Family 2, Type B]
    static class DarkTextBox implements TextBox {
        private final String placeholder;
        DarkTextBox(String placeholder) { this.placeholder = placeholder; }

        @Override
        public void render() {
            System.out.printf("  [Dark  TextBox] (%s)  style: %s%n", placeholder, getStyle());
        }
        @Override public String getPlaceholder() { return placeholder; }
        @Override public String getStyle()        { return "bg:#2D2D2D fg:#CCCCCC border:#555555"; }
    }

    // [ConcreteProduct — Family 2, Type C]
    static class DarkDialog implements Dialog {
        private final String title;
        DarkDialog(String title) { this.title = title; }

        @Override
        public void show(String message) {
            System.out.printf("  [Dark  Dialog] '%s': %s  style: %s%n", title, message, getStyle());
        }
        @Override public String getTitle() { return title; }
        @Override public String getStyle() { return "bg:#252525 fg:#F0F0F0 shadow:rgba(0,0,0,0.5)"; }
    }

    // =========================================================================
    // CONCRETE PRODUCTS — FAMILY 3: High Contrast Theme
    // Adding a THIRD family requires ZERO changes to existing code — OCP
    // =========================================================================

    static class HighContrastButton implements Button {
        private final String label;
        HighContrastButton(String label) { this.label = label; }

        @Override
        public void render() {
            System.out.printf("  [HiCon Button] (%s)  style: %s%n", label, getStyle());
        }
        @Override public String getLabel() { return label; }
        @Override public String getStyle() { return "bg:#000000 fg:#FFFF00 border:#FFFFFF bw:3px"; }
    }

    static class HighContrastTextBox implements TextBox {
        private final String placeholder;
        HighContrastTextBox(String placeholder) { this.placeholder = placeholder; }

        @Override
        public void render() {
            System.out.printf("  [HiCon TextBox] (%s)  style: %s%n", placeholder, getStyle());
        }
        @Override public String getPlaceholder() { return placeholder; }
        @Override public String getStyle()        { return "bg:#000000 fg:#FFFFFF border:#FFFF00 bw:2px"; }
    }

    static class HighContrastDialog implements Dialog {
        private final String title;
        HighContrastDialog(String title) { this.title = title; }

        @Override
        public void show(String message) {
            System.out.printf("  [HiCon Dialog] '%s': %s  style: %s%n", title, message, getStyle());
        }
        @Override public String getTitle() { return title; }
        @Override public String getStyle() { return "bg:#000000 fg:#FFFFFF border:#FFFF00 bw:3px"; }
    }

    // =========================================================================
    // CONCRETE FACTORIES — one per family
    // =========================================================================

    // [ConcreteFactory 1]
    static class LightThemeFactory implements UIThemeFactory {
        @Override public Button  createButton(String l)  { return new LightButton(l); }
        @Override public TextBox createTextBox(String p) { return new LightTextBox(p); }
        @Override public Dialog  createDialog(String t)  { return new LightDialog(t); }
    }

    // [ConcreteFactory 2]
    static class DarkThemeFactory implements UIThemeFactory {
        @Override public Button  createButton(String l)  { return new DarkButton(l); }
        @Override public TextBox createTextBox(String p) { return new DarkTextBox(p); }
        @Override public Dialog  createDialog(String t)  { return new DarkDialog(t); }
    }

    // [ConcreteFactory 3] — added without touching existing code
    static class HighContrastThemeFactory implements UIThemeFactory {
        @Override public Button  createButton(String l)  { return new HighContrastButton(l); }
        @Override public TextBox createTextBox(String p) { return new HighContrastTextBox(p); }
        @Override public Dialog  createDialog(String t)  { return new HighContrastDialog(t); }
    }

    // =========================================================================
    // CLIENT — uses ONLY UIThemeFactory and the three product interfaces
    //          Has no knowledge of Light, Dark, or HighContrast
    // =========================================================================

    // [Client]
    static class Application {
        // [Abstract Product references — never concrete]
        private final Button  loginButton;
        private final Button  cancelButton;
        private final TextBox usernameField;
        private final TextBox passwordField;
        private final Dialog  confirmDialog;

        // [Dependency Injection of AbstractFactory — the "family selector"]
        Application(UIThemeFactory factory) {
            // [Abstract Factory calls — factory decides which family to create]
            this.loginButton   = factory.createButton("Login");
            this.cancelButton  = factory.createButton("Cancel");
            this.usernameField = factory.createTextBox("Enter username");
            this.passwordField = factory.createTextBox("Enter password");
            this.confirmDialog = factory.createDialog("Confirm Action");
        }

        // [Polymorphism] — render() works identically for all themes
        void renderLoginScreen() {
            System.out.println("  --- Login Screen ---");
            usernameField.render();
            passwordField.render();
            loginButton.render();
            cancelButton.render();
        }

        void showConfirmation(String message) {
            confirmDialog.show(message);
        }

        // All components guaranteed to be from the same family
        boolean isConsistentFamily() {
            return loginButton.getStyle().equals(cancelButton.getStyle());
        }
    }

    // =========================================================================
    // DEMO
    // =========================================================================
    public static void main(String[] args) {
        System.out.println("═══ Abstract Factory — UI Theme Demo ═══════════════════════");

        UIThemeFactory[] factories = {
            new LightThemeFactory(),
            new DarkThemeFactory(),
            new HighContrastThemeFactory()
        };
        String[] names = { "Light Theme", "Dark Theme", "High Contrast" };

        for (int i = 0; i < factories.length; i++) {
            System.out.println("\n── " + names[i] + " ─────────────────────────────────────");

            // [Abstract Factory — client uses factory, never concrete classes]
            Application app = new Application(factories[i]);
            app.renderLoginScreen();
            app.showConfirmation("Submit this form?");

            // All buttons/textboxes guaranteed consistent — same family
            System.out.println("  Family consistency: " + app.isConsistentFamily());
        }

        System.out.println("\n── OCP Demonstration ────────────────────────────────────");
        System.out.println("  3 families added without changing Application, UIThemeFactory,");
        System.out.println("  or any existing ConcreteFactory. Only new classes were added.");

        System.out.println("\n── Factory Method returns correct family ─────────────────");
        UIThemeFactory darkFactory = new DarkThemeFactory();
        Button btn = darkFactory.createButton("Test");
        System.out.println("  DarkThemeFactory.createButton() → " + btn.getClass().getSimpleName());
        System.out.println("  Is DarkButton: " + (btn instanceof DarkButton));
    }
}

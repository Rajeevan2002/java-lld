package com.ramkumar.lld.designpatterns.structural.composite.code;

import java.util.ArrayList;
import java.util.List;

// ─────────────────────────────────────────────────────────────────────────────
// Composite Pattern — Scenario A: File System
//
// Problem: A file system contains files and directories. A directory can contain
//          other files OR other directories. We want one operation — getSize() —
//          that works uniformly: calling it on a file returns the file's bytes;
//          calling it on a directory returns the sum of all descendants' bytes,
//          recursively, without the caller knowing whether it holds a file or dir.
//
// Participants:
//   FileSystemComponent  [Component interface] — shared contract for File and Directory
//   File                 [Leaf]      — no children; implements operations directly
//   Directory            [Composite] — holds List<FileSystemComponent>; delegates to children
// ─────────────────────────────────────────────────────────────────────────────

// ── [Component] — the uniform interface that leaf and composite both implement ─
interface FileSystemComponent {
    String getName();
    long getSizeBytes();
    void display(String indent);    // [Recursion] indent grows as we descend the tree
}

// ── [Leaf] — a file; no children; implements every operation directly ─────────
class File implements FileSystemComponent {

    private final String name;       // [Immutable] set at construction
    private final long sizeBytes;    // [Immutable] set at construction

    File(String name, long sizeBytes) {
        this.name = name;
        this.sizeBytes = sizeBytes;
    }

    @Override public String getName()      { return name; }
    @Override public long   getSizeBytes() { return sizeBytes; }   // [Direct] no delegation

    @Override
    public void display(String indent) {
        // [Leaf] just prints itself — no loop over children
        System.out.printf("%s[File] %-20s (%,6d bytes)%n", indent, name, sizeBytes);
    }
}

// ── [Composite] — a directory; holds children; delegates all operations ───────
class Directory implements FileSystemComponent {

    private final String name;       // [Immutable] set at construction

    // [KEY] List typed as the Component interface — NOT as File or Directory.
    // This is what allows arbitrary nesting: a Directory can contain other Directories.
    // If this were List<File>, you could not put a sub-directory inside a directory.
    private final List<FileSystemComponent> children = new ArrayList<>();

    Directory(String name) { this.name = name; }

    // [add] accepts any Component — File or Directory — enabling arbitrary tree depth
    void add(FileSystemComponent component) {
        children.add(component);
    }

    @Override public String getName() { return name; }

    @Override
    public long getSizeBytes() {
        // [Delegation] Ask each child for its size. The child handles its own recursion.
        // No instanceof check needed — every child is a FileSystemComponent.
        long total = 0;
        for (FileSystemComponent child : children) {
            total += child.getSizeBytes();   // [Uniform] same call for File and Directory
        }
        return total;
    }

    @Override
    public void display(String indent) {
        // [Composite] prints its own header, then delegates display() to each child
        System.out.printf("%s[Dir]  %-20s (%,6d bytes)%n", indent, name + "/", getSizeBytes());
        for (FileSystemComponent child : children) {
            child.display(indent + "  ");   // [IndentGrowth] each level adds 2 spaces
        }
    }
}

// ── Demo ─────────────────────────────────────────────────────────────────────
public class FileSystemDemo {

    public static void main(String[] args) {

        // ── 1. Leaf nodes — individual files ──────────────────────────────────
        File main    = new File("Main.java",   61_440);    // 60 KB
        File utils   = new File("Utils.java",  40_960);    // 40 KB
        File readme  = new File("README.md",   51_200);    // 50 KB
        File config  = new File("pom.xml",     10_240);    // 10 KB
        File license = new File("LICENSE",      1_024);    //  1 KB

        // ── 2. Composite nodes — directories ──────────────────────────────────
        Directory src  = new Directory("src");
        src.add(main);
        src.add(utils);     // [Adds Files to Directory]

        Directory docs = new Directory("docs");
        docs.add(readme);   // [Adds File to Directory]

        Directory project = new Directory("project");
        project.add(src);       // [Adds Directory to Directory — arbitrary depth]
        project.add(docs);
        project.add(config);    // [Adds File directly to top-level Directory]
        project.add(license);

        // ── 3. getSize() works uniformly on leaf and composite ─────────────────
        System.out.println("─── Individual file sizes ───");
        // [Polymorphism] — same call on File (leaf)
        System.out.printf("  main.java:   %,d bytes%n", main.getSizeBytes());
        System.out.printf("  src dir:     %,d bytes%n", src.getSizeBytes());    // 60K + 40K
        System.out.printf("  project dir: %,d bytes%n", project.getSizeBytes()); // recursive total
        System.out.println();

        // ── 4. display() prints the full tree with correct indentation ─────────
        System.out.println("─── Full project tree ───");
        project.display("");   // [EntryPoint] root starts with empty indent

        System.out.println();

        // ── 5. Polymorphic — client uses FileSystemComponent[], doesn't know leaf vs dir ──
        System.out.println("─── Polymorphic array (mix of File and Directory) ───");
        FileSystemComponent[] nodes = { main, src, project };
        for (FileSystemComponent node : nodes) {
            // [Uniform] same call regardless of leaf vs composite
            System.out.printf("  %-10s  size=%,d bytes%n", node.getName(), node.getSizeBytes());
        }
    }
}

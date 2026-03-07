package com.ramkumar.lld.designpatterns.structural.proxy.code;

// ─────────────────────────────────────────────────────────────────────────────
// Proxy Pattern — Scenario A: Image Viewer (Virtual Proxy)
//
// Problem:
//   A document viewer opens a slide deck with 200 high-resolution images.
//   Loading each image from disk takes 100–500 ms.
//   Most images are never actually viewed in any given session.
//
//   Eager approach : load all 200 at startup → user waits 20–100 seconds.
//   Virtual Proxy  : load only when display() is called → instant startup.
//
// Participants:
//   Image            [Subject interface]
//   RealImage        [RealSubject — expensive; loads on construction]
//   LazyImageProxy   [Virtual Proxy — defers RealImage creation until first display()]
// ─────────────────────────────────────────────────────────────────────────────

// ── [Subject] — the interface both RealImage and proxy implement ──────────────
// The client works against this type exclusively; it never sees RealImage directly.
interface Image {
    void display();         // [Contract] render the image on screen
    String getFilename();   // [Contract] identify the image without loading it
}

// ── [RealSubject] — the expensive, real object ───────────────────────────────
class RealImage implements Image {

    private final String filename;  // [Immutable] assigned once in constructor

    RealImage(String filename) {
        this.filename = filename;
        loadFromDisk();         // [Expensive] runs at construction time — this is what the proxy avoids
    }

    private void loadFromDisk() {
        // Simulates slow I/O: reading megabytes of pixel data from disk or network.
        System.out.printf("  [DISK]    Loading '%s' from disk%n", filename);
    }

    @Override
    public void display() {
        System.out.printf("  [Display] Showing '%s'%n", filename);
    }

    @Override
    public String getFilename() { return filename; }
}

// ── [Virtual Proxy] — the lightweight stand-in ───────────────────────────────
class LazyImageProxy implements Image {

    private final String filename;  // [OwnState] enough to identify + later construct RealImage
    private RealImage realImage;    // [LazyField] null until first display(); assigned at most once
                                    // Not final — it is written lazily after construction.

    LazyImageProxy(String filename) {
        this.filename = filename;
        // [NoCostHere] No disk I/O. Creating 200 proxies is effectively free.
    }

    @Override
    public void display() {
        if (realImage == null) {
            // [LazyInit] First call only: construct RealImage (triggers disk load).
            realImage = new RealImage(filename);
        }
        // [Delegation] Whether first or subsequent call, delegate rendering to the real image.
        realImage.display();
    }

    @Override
    public String getFilename() {
        // [ProxyAnswers] The proxy knows the filename from construction.
        // No need to load the real image just to answer a metadata question.
        return filename;
    }
}

// ── Demo ─────────────────────────────────────────────────────────────────────
public class ImageViewerDemo {

    public static void main(String[] args) {

        // ── 1. Build a gallery — zero disk reads ──────────────────────────────
        // [Deferred] 5 proxy objects constructed; RealImage never created yet.
        System.out.println("─── Building gallery (5 images) ───");
        Image[] gallery = {
            new LazyImageProxy("slides/title.jpg"),
            new LazyImageProxy("slides/overview.jpg"),
            new LazyImageProxy("slides/architecture.jpg"),
            new LazyImageProxy("slides/benchmarks.jpg"),
            new LazyImageProxy("slides/conclusion.jpg")
        };
        System.out.println("→ Gallery ready. 0 disk reads so far.\n");

        // ── 2. Thumbnail strip — list filenames, still no disk reads ──────────
        // [ProxyAnswers] getFilename() is answered by the proxy without loading.
        System.out.println("─── Thumbnail strip (filenames only) ───");
        for (Image img : gallery) {
            System.out.printf("  %s%n", img.getFilename()); // [Polymorphism] — Image[] loop
        }
        System.out.println("→ Still 0 disk reads.\n");

        // ── 3. User navigates to slide 1 — triggers load on first display() ───
        // [FirstCall] proxy creates RealImage (disk load) then delegates display().
        System.out.println("─── User clicks slide 1: title.jpg ───");
        gallery[0].display();
        System.out.println();

        // ── 4. User clicks the same slide again — no reload ───────────────────
        // [CachedProxy] realImage is no longer null; only display() runs.
        System.out.println("─── User clicks slide 1 again ───");
        gallery[0].display();   // expected: [Display] line only — no [DISK] line
        System.out.println();

        // ── 5. User navigates to slide 3 — loads that slide on demand ─────────
        System.out.println("─── User clicks slide 3: architecture.jpg ───");
        gallery[2].display();
        System.out.println();

        // ── 6. Slides 2, 4, 5 are never viewed — never loaded ────────────────
        System.out.println("─── Session ends — slides 2, 4, 5 were never clicked ───");
        System.out.println("→ Disk reads saved for: "
            + gallery[1].getFilename() + ", "
            + gallery[3].getFilename() + ", "
            + gallery[4].getFilename());
        System.out.println();

        // ── 7. Polymorphic array mixing RealImage and proxies ─────────────────
        // [Polymorphism] Client code uses Image[] — unaware of real vs proxy.
        System.out.println("─── Mixed gallery (RealImage + proxy together) ───");
        Image[] mixed = {
            new RealImage("header/logo.png"),        // [EagerLoad] RealImage loads immediately
            new LazyImageProxy("sidebar/ad.jpg")     // [LazyProxy]  only loads on display()
        };
        System.out.println("→ After construction: RealImage loaded; proxy still deferred.\n");
        for (Image img : mixed) {
            img.display();  // [Uniform] same method call; different behaviour under the hood
        }
    }
}

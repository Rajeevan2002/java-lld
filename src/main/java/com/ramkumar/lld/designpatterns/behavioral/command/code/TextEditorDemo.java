package com.ramkumar.lld.designpatterns.behavioral.command.code;

import java.util.ArrayDeque;
import java.util.Deque;

// ─────────────────────────────────────────────────────────────────────────────
// Command Pattern — Scenario A: Text Editor with Undo
//
// Problem: A text editor must support insert, delete, and bold-toggle operations
//   that can be undone in LIFO order. Hard-coding undo logic in the editor
//   creates a God class that knows about every operation type.
//
// Solution: Wrap each editing operation as a Command object. The command holds
//   a reference to the TextEditor receiver plus all parameters and saved state
//   needed to reverse the operation. The EditorInvoker maintains a history
//   stack and knows only the Command interface.
//
// Participants:
//   Command          [interface]      — execute() + undo()
//   InsertTextCommand [ConcreteCommand] — inserts text; undo deletes it
//   DeleteTextCommand [ConcreteCommand] — deletes text; undo re-inserts it
//   TextEditor        [Receiver]        — knows how to manipulate StringBuilder
//   EditorInvoker     [Invoker]         — history stack; execute + undo
// ─────────────────────────────────────────────────────────────────────────────

// ── [Command interface] ────────────────────────────────────────────────────────
interface Command {
    void execute();
    void undo();
}

// ── [Receiver] — knows how to do the actual work ──────────────────────────────
class TextEditor {

    // [MutableState] StringBuilder is the receiver's mutable state.
    // Commands operate on it via insert/delete/getText.
    private final StringBuilder text = new StringBuilder();

    // [Insert] Inserts s at position pos; shifts existing characters right.
    void insert(int pos, String s) {
        text.insert(pos, s);
        System.out.printf("[Editor] insert(\"%s\" at %d) → \"%s\"%n", s, pos, text);
    }

    // [Delete] Removes characters in range [start, end).
    void delete(int start, int end) {
        String removed = text.substring(start, end);
        text.delete(start, end);
        System.out.printf("[Editor] delete(%d,%d removed \"%s\") → \"%s\"%n",
            start, end, removed, text);
    }

    // [StateQuery] Queries current text — used by commands to compute undo parameters.
    String getText() { return text.toString(); }

    int length() { return text.length(); }
}

// ── [ConcreteCommand 1] — Insert ─────────────────────────────────────────────
class InsertTextCommand implements Command {

    // [ReceiverRef] The command holds a reference to the receiver.
    // The invoker never needs to know about TextEditor.
    private final TextEditor editor;
    private final int        position;
    private final String     textToInsert;

    InsertTextCommand(TextEditor editor, int position, String textToInsert) {
        this.editor       = editor;
        this.position     = position;
        this.textToInsert = textToInsert;
    }

    @Override
    public void execute() {
        editor.insert(position, textToInsert);
    }

    @Override
    public void undo() {
        // [ReverseOperation] Undo by deleting the exact range that was inserted.
        editor.delete(position, position + textToInsert.length());
    }
}

// ── [ConcreteCommand 2] — Delete ─────────────────────────────────────────────
class DeleteTextCommand implements Command {

    private final TextEditor editor;
    private final int        start;
    private final int        end;
    // [SavedState] deletedText is captured at execute() time.
    // It is NOT final because it is assigned in execute(), not the constructor.
    // We cannot capture it at constructor time because the editor's text may
    // change between construction and execution (another command may run first).
    private String deletedText;

    DeleteTextCommand(TextEditor editor, int start, int end) {
        this.editor = editor;
        this.start  = start;
        this.end    = end;
    }

    @Override
    public void execute() {
        // [CaptureBeforeChange] Save the text that will be deleted BEFORE deleting it.
        deletedText = editor.getText().substring(start, end);
        editor.delete(start, end);
    }

    @Override
    public void undo() {
        // [RestoreSavedState] Re-insert the saved deleted text at the original position.
        editor.insert(start, deletedText);
    }
}

// ── [Invoker] — history stack; knows only the Command interface ───────────────
class EditorInvoker {

    // [Stack] Deque used as LIFO stack: push() = addFirst, pop() = removeFirst.
    // LIFO means the most recently executed command is undone first.
    private final Deque<Command> history = new ArrayDeque<>();

    void execute(Command c) {
        c.execute();      // [Delegation] invoker calls execute(); knows nothing about TextEditor
        history.push(c);  // [HistoryPush] save command for potential undo
    }

    void undo() {
        if (history.isEmpty()) {
            System.out.println("[Invoker] Nothing to undo");
            return;
        }
        Command c = history.pop();  // [HistoryPop] most recent command
        c.undo();                   // [Delegation] invoker calls undo(); still knows nothing else
    }

    int historySize() { return history.size(); }
}

// ── Demo ──────────────────────────────────────────────────────────────────────
public class TextEditorDemo {

    public static void main(String[] args) {

        TextEditor   editor   = new TextEditor();
        EditorInvoker invoker = new EditorInvoker();

        // ── 1. Insert commands ──────────────────────────────────────────────
        System.out.println("─── Executing insert commands ───");
        // [ClientCreatesCommand] Client constructs the command with the receiver.
        invoker.execute(new InsertTextCommand(editor, 0, "Hello"));
        invoker.execute(new InsertTextCommand(editor, 5, ", World"));
        invoker.execute(new InsertTextCommand(editor, 12, "!"));
        System.out.println("Text: " + editor.getText());         // Hello, World!
        System.out.println("History size: " + invoker.historySize());  // 3

        // ── 2. Undo inserts in LIFO order ───────────────────────────────────
        System.out.println("\n─── Undoing (LIFO) ───");
        invoker.undo();  // undo "!" insert
        System.out.println("After undo: " + editor.getText());   // Hello, World

        invoker.undo();  // undo ", World" insert
        System.out.println("After undo: " + editor.getText());   // Hello

        // ── 3. Delete command — state capture in execute() ──────────────────
        System.out.println("\n─── Delete + undo ───");
        // Re-insert to have content
        invoker.execute(new InsertTextCommand(editor, 5, ", World!"));
        System.out.println("Before delete: " + editor.getText());

        // [StateCaptureAtRuntime] DeleteTextCommand saves the deleted text inside execute()
        invoker.execute(new DeleteTextCommand(editor, 5, 12));   // deletes ", World"
        System.out.println("After delete: " + editor.getText());

        invoker.undo();  // restores ", World"
        System.out.println("After undo delete: " + editor.getText());

        // ── 4. Empty history ─────────────────────────────────────────────────
        System.out.println("\n─── Undo all, then empty ───");
        while (invoker.historySize() > 0) invoker.undo();
        invoker.undo();   // [EmptyGuard] prints "Nothing to undo"

        // ── 5. Macro: execute a sequence of commands ────────────────────────
        System.out.println("\n─── Macro sequence ───");
        Command[] macro = {
            new InsertTextCommand(editor, 0, "Java"),
            new InsertTextCommand(editor, 4, " LLD"),
            new InsertTextCommand(editor, 8, " Rocks")
        };
        for (Command c : macro) invoker.execute(c);
        System.out.println("Final: " + editor.getText());
    }
}

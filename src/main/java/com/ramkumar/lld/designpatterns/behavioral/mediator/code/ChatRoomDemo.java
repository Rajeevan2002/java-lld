package com.ramkumar.lld.designpatterns.behavioral.mediator.code;

import java.util.ArrayList;
import java.util.List;

/**
 * Worked Example — Mediator Pattern: Chat Room
 *
 * <p><b>Scenario A — Many-to-many messaging through a shared mediator</b>
 *
 * <p>Multiple users communicate inside a chat room. No user holds a reference to
 * any other user — all messages travel through the {@code ChatRoom} mediator.
 * Adding a new user type or removing a user requires zero changes to existing users.
 *
 * <p>Participants:
 * <ul>
 *   <li>{@code ChatMediator}  — [Mediator interface] declares the coordination contract</li>
 *   <li>{@code ChatRoom}      — [ConcreteMediator] owns the user list; routes messages</li>
 *   <li>{@code User}          — [AbstractParticipant] knows only the mediator</li>
 *   <li>{@code RegularUser}   — [ConcreteParticipant] prints received messages</li>
 *   <li>{@code BotUser}       — [ConcreteParticipant] auto-replies to "help" keyword</li>
 * </ul>
 */
public class ChatRoomDemo {

    // ── [MediatorInterface] ────────────────────────────────────────────────────
    interface ChatMediator {
        void send(String message, User sender);   // route a message from sender to others
        void addUser(User user);                  // register a participant
    }

    // ── [ConcreteMediator] ─────────────────────────────────────────────────────
    // The mediator is the ONLY class that knows all participants.
    // Participants know only the mediator — they never reference each other.
    static class ChatRoom implements ChatMediator {

        private final String roomName;
        private final List<User> users = new ArrayList<>();   // [ParticipantRegistry]

        ChatRoom(String roomName) { this.roomName = roomName; }

        @Override
        public void addUser(User user) {
            users.add(user);
            System.out.printf("[%s] %s joined the room.%n", roomName, user.getName());
        }

        // [Broadcast] Deliver message to every user EXCEPT the sender.
        // Uses reference equality (!=) to identify the sender — correct because
        // each User is a distinct object instance.
        @Override
        public void send(String message, User sender) {
            System.out.printf("[%s] %s: \"%s\"%n", roomName, sender.getName(), message);
            for (User u : users) {
                if (u != sender) {          // [ExcludeSender] skip the originator
                    u.receive(message, sender.getName());
                }
            }
        }
    }

    // ── [AbstractParticipant] ──────────────────────────────────────────────────
    // User knows the mediator. User does NOT know any other User.
    // This is the central encapsulation boundary of the pattern.
    static abstract class User {

        protected final ChatMediator mediator;   // [MediatorRef] only link to the outside
        protected final String name;

        // [SelfRegister] Constructor registers with the mediator immediately —
        // no client code needed to call mediator.addUser() separately.
        User(String name, ChatMediator mediator) {
            this.name     = name;
            this.mediator = mediator;
            mediator.addUser(this);   // register on construction
        }

        String getName() { return name; }

        // [SendViaMediator] Participant never calls another participant's receive() directly.
        // It always goes through the mediator, which decides who gets the message.
        void send(String message) {
            mediator.send(message, this);
        }

        // [Receive] Subclasses define how they handle incoming messages
        abstract void receive(String message, String fromName);
    }

    // ── [ConcreteParticipant 1] RegularUser ───────────────────────────────────
    static class RegularUser extends User {

        RegularUser(String name, ChatMediator mediator) {
            super(name, mediator);
        }

        // [Receive] Simply prints the message — no knowledge of the sender's type
        @Override
        void receive(String message, String fromName) {
            System.out.printf("  [%s] ← %s: \"%s\"%n", name, fromName, message);
        }
    }

    // ── [ConcreteParticipant 2] BotUser ───────────────────────────────────────
    // [NewParticipantType] Adding BotUser required ZERO changes to ChatRoom, RegularUser,
    // or any other class — only a new class extending User was needed.
    static class BotUser extends User {

        BotUser(String name, ChatMediator mediator) {
            super(name, mediator);
        }

        // [BotLogic] Auto-replies via the mediator when "help" is detected.
        // The reply goes through mediator.send() — BotUser doesn't know who to reply to.
        @Override
        void receive(String message, String fromName) {
            System.out.printf("  [%s-BOT] ← %s: \"%s\"%n", name, fromName, message);
            if (message.toLowerCase().contains("help")) {
                // [ReplyThroughMediator] Bot replies to the room, not directly to fromName
                send("Auto-reply: Help is on the way!");
            }
        }
    }

    // ── main() ────────────────────────────────────────────────────────────────
    public static void main(String[] args) {

        ChatRoom room = new ChatRoom("GeneralChannel");

        // ── [ChainAssembly] Register participants by constructing them ─────────
        // Each constructor calls mediator.addUser(this) — client needs no extra setup
        User alice = new RegularUser("Alice", room);
        User bob   = new RegularUser("Bob",   room);
        User carol = new RegularUser("Carol", room);
        User helpBot = new BotUser("HelpBot", room);

        // ── [NoDirectCoupling] Users communicate only via send() → mediator ───
        System.out.println("\n── Alice sends a greeting ──");
        alice.send("Hello everyone!");
        // Alice does NOT receive her own message (ExcludeSender)

        System.out.println("\n── Bob replies ──");
        bob.send("Hi Alice! How are you?");

        // ── [BotReaction] BotUser reacts to the keyword, replies through mediator ─
        System.out.println("\n── Carol asks for help ──");
        carol.send("Anyone know how this works? help!");
        // HelpBot receives the message and auto-replies to the room

        // ── [IsolatedSend] Sender does not receive its own message ─────────────
        System.out.println("\n── Direct send test: Alice sends again ──");
        alice.send("Testing — I should NOT see this myself.");
        // Only Bob, Carol, HelpBot receive this. Alice does not.

        // ── [NewUserNoRewiring] Adding a new user — zero changes to existing users ─
        System.out.println("\n── Dave joins ──");
        User dave = new RegularUser("Dave", room);
        dave.send("Hey all, just joined!");
        // Alice, Bob, Carol, HelpBot all receive Dave's message automatically
    }
}

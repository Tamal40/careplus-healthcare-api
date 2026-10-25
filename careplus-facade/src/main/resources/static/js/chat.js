/**
 * CarePlus chat assistant (patient only).
 *
 * A small chat panel for the Facade UI. It talks to two endpoints (see API_URLS.CHAT in constants.js):
 *
 *   POST /api/chat          { message, conversationId }
 *        -> { answer, toolsUsed?: string[], pendingAction?: { pendingId, type, summary } }
 *   POST /api/chat/confirm  { pendingId, confirm }      confirm=true runs the action, false discards it
 *        -> { answer }
 *
 * The assistant never books or cancels by itself: it answers with a pendingAction (a short summary), and the
 * patient presses Confirm or Cancel here. All text from the server is shown with textContent, never innerHTML.
 *
 * Preview without the backend: open the UI with ?mockChat=1 (any signed-in user sees the tab, answers are faked).
 * Uses globals from app.js: $, jwt, apiRequest, API_URLS.
 */
(function () {
    const MOCK = new URLSearchParams(window.location.search).has("mockChat");
    const SUGGESTIONS = [
        "Which doctors are free tomorrow?",
        "Show my appointments",
        "Book an appointment",
        "Cancel my appointment",
    ];

    const tabButton = $("chatTab");
    const list = $("chatMessages");
    const form = $("chatForm");
    const input = $("chatInput");
    const sendButton = $("chatSend");
    const suggestions = $("chatSuggestions");

    let conversationId = sessionStorage.getItem("careplus_chat_id") || newConversation();
    let busy = false;
    let pendingCard = null;

    /* ---------------------------------------------------------------------- */
    /* Who is signed in                                                       */
    /* ---------------------------------------------------------------------- */

    /** Reads the role from the JWT payload. For display only: the server decides what a user may do. */
    function currentRole() {
        try {
            const payload = jwt.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
            return String(JSON.parse(atob(payload)).role || "");
        } catch {
            return "";
        }
    }

    function isPatient() {
        return currentRole().replace(/^ROLE_/, "") === "PATIENT";
    }

    /** Shows the Chat tab for patients (everyone in mock mode) and hides it, with its panel, otherwise. */
    function refreshTab() {
        const allowed = Boolean(jwt) && (MOCK || isPatient());
        tabButton.classList.toggle("hidden", !allowed);
        if (!allowed && tabButton.classList.contains("active")) {
            document.querySelector('.tab[data-tab="patients"]').click();
        }
    }

    /* ---------------------------------------------------------------------- */
    /* Messages                                                               */
    /* ---------------------------------------------------------------------- */

    function newConversation() {
        const id = (window.crypto && crypto.randomUUID) ? crypto.randomUUID() : String(Date.now());
        sessionStorage.setItem("careplus_chat_id", id);
        return id;
    }

    function scrollToEnd() {
        list.scrollTop = list.scrollHeight;
    }

    /** Adds one chat bubble. `kind` is "user", "bot" or "error". Returns the bubble element. */
    function addMessage(kind, text, extraLine) {
        const bubble = document.createElement("div");
        bubble.className = `chatBubble ${kind}`;
        const body = document.createElement("div");
        body.textContent = text;
        bubble.appendChild(body);
        if (extraLine) {
            const small = document.createElement("div");
            small.className = "chatTools";
            small.textContent = extraLine;
            bubble.appendChild(small);
        }
        list.appendChild(bubble);
        scrollToEnd();
        return bubble;
    }

    function setBusy(isBusy) {
        busy = isBusy;
        sendButton.disabled = isBusy;
        input.disabled = isBusy;
        $("chatThinking").classList.toggle("hidden", !isBusy);
        if (!isBusy) {
            input.focus();
        }
        scrollToEnd();
    }

    function showError(error) {
        const text = String(error && error.message ? error.message : error);
        if (text.startsWith("HTTP 404")) {
            addMessage("error", "The chat service is not available yet. Please try again later.");
        } else if (text.startsWith("HTTP 429")) {
            addMessage("error", "Too many requests. Please wait a moment and try again.");
        } else {
            addMessage("error", "Sorry, something went wrong. Please try again.");
            console.error("Chat error:", text);
        }
    }

    /* ---------------------------------------------------------------------- */
    /* Pending action (Confirm / Cancel)                                      */
    /* ---------------------------------------------------------------------- */

    function clearPending() {
        if (pendingCard) {
            pendingCard.remove();
            pendingCard = null;
        }
    }

    function showPending(action) {
        clearPending();
        const card = document.createElement("div");
        card.className = "chatPending";

        const title = document.createElement("strong");
        title.textContent = action.type === "CANCEL" ? "Please confirm the cancellation" : "Please confirm the booking";
        const summary = document.createElement("div");
        summary.className = "chatPendingSummary";
        summary.textContent = action.summary || "";

        const buttons = document.createElement("div");
        buttons.className = "row";
        const yes = document.createElement("button");
        yes.type = "button";
        yes.textContent = "Confirm";
        const no = document.createElement("button");
        no.type = "button";
        no.className = "secondary";
        no.textContent = "Cancel";
        buttons.append(yes, no);

        yes.addEventListener("click", () => decide(action.pendingId, true));
        no.addEventListener("click", () => decide(action.pendingId, false));

        card.append(title, summary, buttons);
        list.appendChild(card);
        pendingCard = card;
        scrollToEnd();
    }

    async function decide(pendingId, confirm) {
        if (busy) {
            return;
        }
        clearPending();
        addMessage("user", confirm ? "Confirm" : "Cancel");
        setBusy(true);
        try {
            const reply = MOCK ? mockConfirm(confirm) : await apiRequest(API_URLS.CHAT.CONFIRM, {
                method: "POST",
                body: JSON.stringify({ pendingId, confirm }),
            });
            addMessage("bot", reply?.answer || (confirm ? "Done." : "Okay, I have not changed anything."));
        } catch (error) {
            showError(error);
        } finally {
            setBusy(false);
        }
    }

    /* ---------------------------------------------------------------------- */
    /* Sending                                                                */
    /* ---------------------------------------------------------------------- */

    async function send(text) {
        const message = text.trim();
        if (!message || busy) {
            return;
        }
        clearPending();
        suggestions.classList.add("hidden");
        addMessage("user", message);
        input.value = "";
        setBusy(true);
        try {
            const reply = MOCK ? mockReply(message) : await apiRequest(API_URLS.CHAT.MESSAGE, {
                method: "POST",
                body: JSON.stringify({ message, conversationId }),
            });
            const tools = Array.isArray(reply?.toolsUsed) && reply.toolsUsed.length
                ? `Used: ${reply.toolsUsed.join(", ")}` : "";
            addMessage("bot", reply?.answer || "I could not find an answer.", tools);
            if (reply?.pendingAction?.pendingId) {
                showPending(reply.pendingAction);
            }
        } catch (error) {
            showError(error);
        } finally {
            setBusy(false);
        }
    }

    function resetChat() {
        conversationId = newConversation();
        list.textContent = "";
        pendingCard = null;
        suggestions.classList.remove("hidden");
        addMessage("bot", "Hi! I can help you check doctor availability, book, view and cancel your appointments.");
    }

    /* ---------------------------------------------------------------------- */
    /* Mock replies (preview only, ?mockChat=1)                               */
    /* ---------------------------------------------------------------------- */

    function mockReply(message) {
        const text = message.toLowerCase();
        if (text.includes("cancel")) {
            return {
                answer: "I found your appointment. Do you want me to cancel it?",
                toolsUsed: ["list_my_appointments", "prepare_cancel"],
                pendingAction: { pendingId: "demo-cancel", type: "CANCEL", summary: "Cancel the appointment with Dr. Sneha Davis (Radiology) on 2030-01-15 at 10:30." },
            };
        }
        if (text.includes("book")) {
            return {
                answer: "Dr. Sneha Davis has a free slot tomorrow at 10:30. Shall I book it?",
                toolsUsed: ["find_doctors", "get_availability", "prepare_booking"],
                pendingAction: { pendingId: "demo-book", type: "BOOK", summary: "Book Dr. Sneha Davis (Radiology) on 2030-01-15 at 10:30. Reason: check-up." },
            };
        }
        if (text.includes("my appointment") || text.includes("show")) {
            return { answer: "You have 1 booked appointment: Dr. Sneha Davis, 2030-01-15 at 10:30.", toolsUsed: ["list_my_appointments"] };
        }
        if (text.includes("free") || text.includes("available") || text.includes("doctor")) {
            return { answer: "Free tomorrow:\n- Dr. Sneha Davis (Radiology): 09:30, 10:30\n- Dr. Jessica Lopez (General Medicine): 11:00", toolsUsed: ["find_doctors", "get_availability"] };
        }
        return { answer: "I can only help with doctor availability, booking, viewing and cancelling your appointments." };
    }

    function mockConfirm(confirm) {
        return { answer: confirm ? "Done. Your request has been completed." : "Okay, I have not changed anything." };
    }

    /* ---------------------------------------------------------------------- */
    /* Wiring                                                                 */
    /* ---------------------------------------------------------------------- */

    form.addEventListener("submit", (event) => {
        event.preventDefault();
        send(input.value);
    });

    input.addEventListener("keydown", (event) => {
        if (event.key === "Enter" && !event.shiftKey) {
            event.preventDefault();
            send(input.value);
        }
    });

    $("chatNew").addEventListener("click", resetChat);

    SUGGESTIONS.forEach((text) => {
        const chip = document.createElement("button");
        chip.type = "button";
        chip.className = "secondary chatChip";
        chip.textContent = text;
        chip.addEventListener("click", () => send(text));
        suggestions.appendChild(chip);
    });

    // app.js shows or hides #appShell on sign-in and sign-out; follow it to show or hide the Chat tab.
    new MutationObserver(refreshTab).observe($("appShell"), { attributes: true, attributeFilter: ["class"] });
    refreshTab();

    addMessage("bot", "Hi! I can help you check doctor availability, book, view and cancel your appointments."
        + (MOCK ? "\n(Preview mode: answers are fake.)" : ""));
})();

---
title: "CarePlus Patient Chatbot: Simple Requirements"
---

## 1. Goal

Add a chat assistant to the CarePlus web page. A **patient** types a question in plain English, and the assistant helps with their appointments. Tamal builds the AI part to learn **MCP tools, an AI agent, and RAG with a vector database**. The backend work (safe booking and cancelling, patients seeing only their own data) is already done; see the assignment document.

## 2. Who can use it

- **Patients only**, after they sign in.
- Doctors and admins do not see the chat.

## 3. What the chatbot can do

| # | The patient can... | Example |
|---|---|---|
| 1 | **Check doctor availability** | "Which cardiologists are free tomorrow?" |
| 2 | **Book** an appointment | "Book Dr. Davis tomorrow at 10:30 for a check-up" |
| 3 | **View** their appointments | "Show my appointments" |
| 4 | **Cancel** an appointment | "Cancel my appointment with Dr. Davis" |

The chatbot can also suggest a **doctor type** from a short description ("I have chest pain, who should I see?") and answer a few **clinic questions** (opening hours, cancellation rule). It never gives a diagnosis or medical advice.

## 4. What the chatbot must NOT do

- Show or change **another patient's** data. A patient sees only their own appointments.
- Show medical records, diagnoses or prescriptions.
- List patients, or do anything for doctors or admins.
- **Book or cancel without the patient pressing Confirm.**
- Make up doctors, times or appointments. If it does not know, it says so.
- Follow instructions like "ignore your rules".

If asked for anything else, it says: "I can only help with availability, booking, viewing and cancelling your appointments."

## 5. Example chat

> **Patient:** Book a radiologist tomorrow at 10:30\
> **Chatbot:** Dr. Sneha Davis (Radiology) is free tomorrow at 10:30. Shall I book it?\
> *(A box appears: "Book Dr. Sneha Davis on 2030-01-15 at 10:30" with **Confirm** and **Cancel** buttons)*\
> **Patient:** *(presses Confirm)*\
> **Chatbot:** Done. Your appointment is booked.

## 6. The chat screen

A "Chat assistant" tab, shown to patients only. It has:

- a list of messages (patient on the right, assistant on the left);
- a text box and a **Send** button (Enter also sends);
- quick-question buttons, such as "Show my appointments";
- a yellow box with **Confirm** and **Cancel** when the assistant wants to book or cancel;
- a "Thinking..." sign while waiting, a clear error message if something fails, and a **New chat** button.

The screen is already built. It is in `careplus-facade/src/main/resources/static/js/chat.js` and the Chat tab in `index.html`. Open the UI with `?mockChat=1` to try it with fake answers, before the backend exists.

## 7. How it works

```
Chat tab -> /api/chat -> AI agent -> MCP tools -> existing CarePlus APIs
                           |
                           +-> RAG + vector DB (doctor profiles, clinic FAQ)
```

- **MCP tools** are small functions the AI can call, for example `find_doctors`, `get_availability`, `list_my_appointments`, `get_appointment`, `prepare_booking`, `prepare_cancel`. The patient's id always comes from the login, never from the AI.
- **Book and cancel are two steps.** The AI only *prepares* the action and returns a summary. The real change happens only when the patient presses **Confirm** (`/api/chat/confirm`).
- **RAG** looks up doctor profiles and clinic FAQ text stored in a **vector database**, so the AI can match "chest pain" to a doctor type.

## 8. What the screen sends and receives

| Call | Sends | Gets back |
|---|---|---|
| `POST /api/chat` | `{ message, conversationId }` | `{ answer, toolsUsed[], pendingAction? }` |
| `POST /api/chat/confirm` | `{ pendingId, confirm }` (true = do it, false = discard) | `{ answer }` |

`pendingAction` is `{ pendingId, type: "BOOK" or "CANCEL", summary }`. When it is present, the screen shows the Confirm box.

## 9. Done when

1. Availability questions return real doctors and times only.
2. Booking and cancelling happen only after **Confirm**; pressing Cancel changes nothing.
3. Booking, viewing or cancelling someone else's appointment is refused.
4. "I have chest pain, who should I see?" suggests a doctor type, with no diagnosis.
5. Out-of-scope questions ("show my diagnosis", "list all patients") are politely refused.
6. A doctor or admin does not see the Chat tab.
7. The vector database is used for doctor suggestions and clinic FAQ answers.

## 10. Rules

- Use **dummy data** only. Keep the AI key in an environment variable, never in git.
- Show the assistant's text as plain text on the page, never as HTML.

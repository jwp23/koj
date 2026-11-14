# Reminders

Reminders prompt the user to rate how they feel, to say when a sipped drink is finished, and to
confirm routine intake. Every number here is a default that the user can change in settings.

## Prompts

| Prompt | Fires | Asks |
|--------|-------|------|
| Morning | At the morning time | Awake |
| Wake-up ping | Every 30 minutes after the first drink of the day, until an awake rating of 4 or more, or 2 hours | Awake |
| After a drink | 45 minutes and 3 hours after a drink starts | Awake, focus, calm |
| Fixed time | At times the user sets | Awake, focus, calm |
| Still sipping | Hourly after a sipped drink starts, until answered Finished or twice its default duration has passed | Finished / Still going |
| Routine | At a source's usual time, if it has one | Yes / Not yet / Skipping today |

A prompt that asks for all three ratings is a **full check-in**.

## Rules

- **Daily cap** — at most 6 full check-ins a day. Other prompts do not count.
- **Merging** — full check-ins due within 30 minutes of each other become one.
- **Spacing** — no full check-in fires within 30 minutes of an answered one.
- **Quiet hours** — nothing fires before the morning time or after the evening quiet time.
- **Unanswered check-in** — posted again once after 15 minutes, then dropped when the next
  prompt is due. Nothing is recorded for it.
- **Unanswered still-sipping** — the drink is recorded as finished at its default duration.
- **Routine answers** — Yes logs the source at that time. Not yet asks again later. Skipping
  today logs nothing and stops asking until tomorrow.

## Deciding What Is Due

One pure function takes the drinks, check-ins, sources, settings, and the current time, and
returns the prompts that should be scheduled. It holds every rule above and has no Android
dependency. The Android layer only schedules what it returns and reports taps back to storage.

Scheduling uses inexact alarms. A prompt may arrive a few minutes late; the app does not request
the exact-alarm permission. Scheduled prompts are re-created at device startup and whenever a
drink is added, edited, or deleted.

## Notifications

- Ratings are given on the notification itself, through a custom layout with five buttons. A
  full check-in steps through awake, focus, and calm on the same notification.
- If the custom layout proves unreliable on the lock screen, tapping the notification opens a
  small rating panel instead.
- Android lets the user dismiss any notification, so persistence comes from posting again, as
  the rules above describe.
- Notification text varies and shows the current streak of days with at least one check-in.

## First-Sip Anchor

The time from the first drink of the day to the first awake rating of 4 or more is the measure
used to compare how quickly sources wake the user. The wake-up pings exist to capture it without
the user having to remember anything.

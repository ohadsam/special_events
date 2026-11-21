# Public API for other apps (read only)

Used by **ClockWise** (`ohadsam/clock_weather_widget`) to show birthdays and other special events in its widget. Nothing can be written through it.

`content://com.ohadsam.specialevents.public/upcoming?days=N` (N = 1..366, default 30)

| Column | Meaning |
|---|---|
| `_id` | row number |
| `title` | event title (max 80 chars); a generic text when the privacy switch "hide details" is on |
| `type` | `EventType` name (BIRTHDAY, ANNIVERSARY, YAHRZEIT, ...) |
| `epoch_day` | the date of the next occurrence (days since 1970-01-01) |
| `days_left` | days from today |

Access: the caller declares and the user grants the runtime permission `com.ohadsam.specialevents.permission.READ_UPCOMING` (protection level dangerous; label and description are in `strings.xml`). At most 50 rows. The provider reads the same data as the home-screen widgets (`EventTimeline.upcoming`). Changing the columns or the authority breaks ClockWise: update `docs/SPECIAL_EVENTS_INTEGRATION.md` in that repo too.

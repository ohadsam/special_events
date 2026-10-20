# Security and privacy (Special Events)

What the app protects, what it does not, and how each risk is handled. Updated at every batch (release-checklist step 5).

## Data held
Names, dates, notes, phone numbers and e-mail addresses of the user's contacts; reminder rules; greeting history; photos; checklists and gift budgets.
All of it lives in the app's private storage (Room database, DataStore, `files/photos`, `cache/`). Nothing is sent to any server: the app declares **no INTERNET permission** and the print WebView blocks network loads.

## Threats and answers
| Threat | Answer |
|---|---|
| Someone picks up the unlocked phone | Optional app lock: the phone's own PIN / pattern / password / biometrics via the system confirm screen (never read or stored by the app); timeout 0/30/60/300 s; locks on cold start. |
| Screenshots, recent-apps thumbnail | Optional `FLAG_SECURE`. |
| Names shown on a locked screen | Notifications are `VISIBILITY_PRIVATE` with a generic public version. Widgets do show events on the home screen - documented in Settings. |
| Malicious or corrupt backup / share file | Size cap (5M characters), format+version check, per-field clamping, unknown fields ignored, invalid entries skipped and counted; auto-send is forced off on import; device-specific sound and template ids are dropped; preview and explicit confirmation before anything is written; import in one transaction. |
| Backup file read by others | Warned in the UI; optional AES-256-GCM encryption with PBKDF2-HMAC-SHA256 (210k iterations, random salt/IV), tamper-detected. |
| CSV formula injection | Cells starting with `= + - @` are prefixed (phone numbers excepted). |
| HTML injection in print/share | Every user string escaped; colours validated; only `data:image/` URIs accepted; WebView has JavaScript, file and network access off. |
| Other apps calling our components | Only `MainActivity` (launcher + validated VIEW; `content:` URIs only, no `file:`; a foreign `route` extra is length-limited, cannot open the import screen and a route that does not exist is dropped), the widget receivers/config activity (required by the launcher; the config activity refuses an invalid widget id) are exported. Boot/date receivers are not exported. All `PendingIntent`s are `FLAG_IMMUTABLE`. |
| Daily summary notification | Off by default; names appear like in ordinary reminders (same notification channels). No new permission (POST_NOTIFICATIONS only). |
| Undo after delete | The deleted event is held in memory only (not written anywhere) until the Undo offer ends; its photo file is deleted when the offer expires. |
| Widgets on a locked phone | Setting "hide details in widgets" renders a generic title instead of names and checklist items (off by default). |
| Gallery photo in a greeting | Chosen with the system photo picker (no storage permission); only that one URI is handed to the target app, read-only and temporary; the app keeps no copy. |
| File sharing | `FileProvider` limited to cache sub-folders `voice/`, `backup/`, `share/`, granted per share with read-only URI permission. |
| Unwanted SMS cost / spam | Automatic SMS is opt-in per rule, needs the SEND_SMS permission, shows the count and cost warning for bulk sends, is capped at 30 recipients and records each recipient to avoid double sends. |
| Logs leaking personal data | Log lines carry ids and counts only, never names, phones or message text (audited in batch 14). |
| Vulnerable libraries | Dependabot weekly (gradle + actions); lint and detekt in CI. |

## Permissions (all requested in context, all optional)
POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM, RECEIVE_BOOT_COMPLETED, VIBRATE (reminders) · READ_CONTACTS (import, on demand) · RECORD_AUDIO (voice greeting, on tap) · SEND_SMS (automatic SMS, per rule) · READ_CALENDAR / WRITE_CALENDAR (calendar sync screen). No location, no storage, no internet.

## Known limits / ideas
- The database is not encrypted at rest beyond Android's file-based encryption (SQLCipher with a Keystore key is an option if required).
- Authentication uses the system confirm screen; a BiometricPrompt with crypto binding would be stronger.
- Android Auto Backup is disabled (`allowBackup=false`); the user's own backup file is the way to move data.

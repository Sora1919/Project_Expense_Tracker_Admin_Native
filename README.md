# Project Expense Tracker (Admin) — Coursework 1

This repository contains the **Admin (native Android)** application for Coursework 1.
The Admin app is used to **create and manage projects and expenses locally (SQLite/Room)**, provide **search and project details**, and **upload all project data to Firebase Realtime Database** (cloud sync).

---

## Features (Admin App)

### Feature (a) — Create Project (with validation + confirmation)

* Add new project with required/optional fields
* Input validation (required fields, numeric budget, etc.)
* Confirmation step before saving

### Feature (b) — Store & Manage Projects (SQLite)

* Projects stored locally using **Room (SQLite)**
* View all projects in a **RecyclerView**
* Edit / Delete project
* Reset DB (delete all projects)

### Feature (c) — Manage Expenses per Project (SQLite)

* Expenses linked to a project (projectId)
* Add / Edit / Delete expenses for a project
* Expense list using RecyclerView

### Feature (d) — Search + Project Detail

* Live search by project name/description
* Advanced filters (status, owner, date range)
* Project detail page (full details + expenses summary)

### Feature (e) — Upload to Cloud (Firebase Realtime Database)

* Upload **all projects (and expenses)** from local Room DB to Firebase RTDB
* Checks network availability before upload
* Uses **Firebase Anonymous Authentication** to satisfy database rules

### Feature (f) — Dashboard Summary (additional feature)

* Dashboard view showing:

  * total projects and status counts
  * total expenses (grouped by currency)
  * per-project spent vs budget (currency filter)
  * last cloud upload time

### Extra (optional enhancement)

* **Sync Expenses from Cloud** (manual refresh)
  Pulls user-submitted expenses from Firebase RTDB into Room.

---

## Tech Stack

* **Kotlin + XML**
* **Room (SQLite)** for local storage
* **RecyclerView** for lists
* **Firebase Realtime Database** for cloud upload
* **Firebase Anonymous Auth** for secure read/write rules

---

## Firebase Realtime Database Structure

Example structure used in cloud:

```
projects/
  PJ-001/
    projectName: "..."
    ...
    expenses/
      EXP-123/
        amount: 100
        currency: "MMK"
        ...
userFavourites/
  <uid>/
    PJ-001: true
```

---

## How to Run (Admin App)

1. Open the Admin project in **Android Studio**
2. Sync Gradle
3. Run on emulator or Android device

### Firebase Setup (Admin)

1. Create Firebase Project
2. Enable:

   * **Realtime Database**
   * **Authentication → Anonymous**
3. Add Android app to Firebase and download `google-services.json`
4. Put it inside: `app/google-services.json`
5. Realtime Database Rules (coursework version):

```json
{
  "rules": {
    ".read": "auth != null",
    ".write": "auth != null"
  }
}
```

---

## Demo Flow (Suggested)

1. Create projects locally
2. Add expenses
3. Use search + detail page
4. Upload all projects to Firebase RTDB
5. (Optional) Add expense in user app → sync back in admin using “Sync from Cloud”

---

## Notes

* During development, destructive migrations may be enabled when schema changes.
* Currency handling: budget does not include currency, so dashboard provides a currency filter for “spent vs budget”.

---

## Author

**Kaung Set Linn**
```
https://github.com/Sora1919
```
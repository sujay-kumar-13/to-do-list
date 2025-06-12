# 📝 To Do List – Android App

A feature-rich and beautifully designed Android application to help users manage their daily tasks efficiently. Built with Firebase integration, reminders, and sleek UI transitions, this app is perfect for organizing life on the go.

---

## 📱 Overview

**To Do List** is a task management Android app that allows users to add, update, and delete their tasks while setting reminders for important events. With Firebase Firestore support, users enjoy real-time sync and persistent storage.

---

## ✨ Features

- ➕ Add new tasks with title and optional description
- ✔️ Mark tasks as complete/incomplete
- 🗑️ Delete tasks with long-press action
- ✏️ Edit tasks including reminder and repetition options
- 🔔 Schedule reminders with **AlarmManager** and **WorkManager**
- 🌐 Online task storage using **Firebase Firestore**
- 🧼 Clean Material UI with reusable card styles and input fields
- 🚀 Splash screen with smooth navigation to the main app

---

## 🧰 Tech Stack
- **Language**: Java
- **IDE**: Android Studio
- **Database**: Firebase FireStore
- **UI**: Material Design Components
- **Architecture**: MVVM
- **Notifications**: AlarmManager, WorkManager

---

## 📁 Project Structure

```
ToDoList/
├── app/
│   └── src/
│       └── main/
│           ├── java/com/sujay/apps/todolist/
│           │   ├── AddTaskFragment.java
│           │   ├── Helper.java
│           │   ├── LoginFragment.java
│           │   ├── MainActivity.java
│           │   ├── NotificationHelper.java
│           │   ├── NotificationReceiver.java
│           │   ├── ReminderWorker.java
│           │   ├── SettingsFragment.java
│           │   ├── TaskListFragment.java
│           │   └── ThemeUtils.java
│           ├── res/
│           │   ├── layout/
│           │   │   ├── activity_main.xml
│           │   │   ├── content_main.xml
│           │   │   ├── fragment_add_task.xml
│           │   │   ├── fragment_login.xml
│           │   │   ├── fragment_settings.xml
│           │   │   ├── fragment_task_list.xml
│           │   │   └── task_row.xml
│           │   ├── drawable/
│           │   ├── values/
│           │   └── AndroidManifest.xml
```

---

## 🚀 Getting Started

To get a local copy up and running, follow these simple steps:

### ✅ Prerequisites

- Android Studio (Iguana | 2023.2.1 or newer)
- JDK 17+
- Android SDK (API 26+)
- Firebase Project with Firestore enabled
- `google-services.json` downloaded from Firebase Console

### 📥 Clone the Repository

```bash
git clone https://github.com/sujay-kumar-13/to-do-list.git
cd to-do-list
```

### 🔧 Set Up Firebase

1. Go to the Firebase Console and create a new Android project.
2. Register your Android package name (e.g., `com.sujay.apps.todolist`).
3. Download the `google-services.json` file.
4. Place it in the `app/` directory.

### ▶️ Run the App

1. Open the project in **Android Studio**.
2. Sync Gradle by clicking "Sync Project with Gradle Files".
3. Select a device/emulator and hit the **Run** button (green triangle).

---

## 🎮 Usage Guide

- Tap the ➕ button to create a new task.
- Tap on a task to edit its content, set a reminder or make it recurring.
- Long-press on tasks to select and delete multiple items.
- Check off tasks to mark them as completed.

---

## 🧩 Future Enhancements

- 📱 Add home screen widget for quick access
- ☁️ Enable multi-device sync and backup options
- 🗃️ Category-wise task organization

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

## 👤 Author

Developed by [Sujay Kumar](https://github.com/sujay-kumar-13)  
If you like this project, don't forget to ⭐ the repo!
# To Do List Android app

## Overview
The **To Do List** is an android application designed to manage tasks creations, deletion, changes and reminders.The system provides an intuitive interface for users to create tasks and an edit page for tasks to manage changes, repetition and reminders.

## Features
- Add new tasks with titles and descriptions
- Mark tasks as completed
- Delete tasks
- Online storage using Firebase FireStore
- Splash screen with smooth transition to the main interface
- Custom styles for input fields and task cards

## Tech Stack
- **Language**: Java
- **Framework**: Android Studio
- **Database**: Firebase FireStore
- **UI**: Material Design Components
- **Architecture**: MVVM

## Project Structure
```
ToDoList/
├── app/
│ └── src/
│ └── main/
│ ├── java/com/example/todolist/
│ │ ├── AddTaskFragment.java
│ │ ├── Helper.java
│ │ ├── LoginFragment.java
│ │ ├── MainActivity.java
│ │ ├── NotificationHelper.java
│ │ ├── NotificationReceiver.java
│ │ ├── ReminderWorker.java
│ │ ├── SettingsFragment.java
│ │ ├── TaskListFragment.java
│ │ └── ThemeUtils.java
│ ├── res/
│ │ ├── layout/
│ │ │ ├── activity_main.xml
│ │ │ ├── content_main.xml
│ │ │ ├── fragment_add_task.xml
│ │ │ ├── fragment_login.xml
│ │ │ ├── fragment_settings.xml
│ │ │ ├── fragment_task_list.xml
│ │ │ └── task_row.xml
│ │ ├── drawable/
│ │ ├── values/
│ │ └── AndroidManifest.xml
```

## Getting Started

To get a local copy up and running, follow these simple steps.

### Prerequisites

*   Android Studio (Latest stable version recommended - e.g., Iguana | 2023.2.1 or newer)
*   JDK (Java Development Kit) 17 or newer (usually bundled with Android Studio)
*   Android SDK with appropriate API levels (e.g., API 26+)
*   An Android Emulator or a physical Android device.
*   A Firebase project set up and the `google-services.json` file.

### How to clone the repo

1.  Clone the repo:
   ```sh
   git clone https://github.com/sujay-kumar-13/to-do-list.git
   cd to-do-list
   ```
2. Open in Android Studio
   - Open Android Studio.
   - Select "Open an Existing Project".
   - Navigate to the cloned `to-do-list` directory and open it.
3.  Add `google-services.json`
   - Go to your Firebase project console.
   - Download the `google-services.json` file for your Android app.
   - Place this file in the `app/` directory of your project.
4.  Build the project 
   - Android Studio should automatically sync and build the project. If not, click "Sync Project with Gradle Files" (the elephant icon) and then "Build" > "Make Project".
5.  Run the app
   - Select an emulator or connect a physical device.
   - Click the "Run" button (green play icon) in Android Studio.

## Usage
 - Tap the '+' button to add a new task.
 - Press and hold to select tasks and delete them.
 - Tap on a task to edit its details.
 - Check the checkbox to mark a task as complete.

## Future Enhancements
- Add widget to manage directly from home screen.
- Multiple device support.

## License
This project is licensed under the MIT License.

## Author
Developed by [Sujay Kumar](https://github.com/sujay-kumar-13).

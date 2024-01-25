# Location Reminder Project - README

## Project Overview
The Location Reminder Project is an Android application designed to set geofence-based reminders at specific points of interest (POIs). It integrates Firebase Authentication for user accounts, offers a customized map view for selecting locations, and sends notifications when a user reaches a designated POI.

## Features

### User Authentication
- **Login and Registration**: Users can log in via email or Google Account. Unregistered users are navigated to the Registration screen.
- **Firebase Integration**: Utilizes Firebase Authentication and Firebase UI for managing user accounts.

### Map View
- **Current Location Display**: Shows the user's current location on a map, requiring location permission.
- **POI Selection and Reminders**: Users can select POIs on the map to set reminders, including a title and description.
- **Map Styling**: Enhanced map aesthetics using the map styling wizard, with options to change map types.
- **POI Notifications**: Notifications are displayed when the user enters the geofence of a selected POI, independent of the app's open state.

### Reminders
- **Creating Reminders**: Users can create reminders with a title and description, using live data and data binding.
- **List View of Reminders**: Displays all reminders from the local database with options to navigate to the reminder creation screen.
- **Reminder Details**: On clicking a notification, details of the corresponding reminder are displayed.

### Testing
- **Architecture**: Follows MVVM and uses Dependency Injection (Koin) for structuring the app.
- **ViewModel Testing**: Includes tests for ViewModel, LiveData, and Coroutines.
- **UI and Fragment Navigation Testing**: Utilizes Espresso and Mockito for UI testing and fragment navigation.
- **DAO and Repository Testing**: Involves tests using Room.inMemoryDatabaseBuilder and covers various scenarios including data insertion and retrieval.

### Code Quality
- **Kotlin Best Practices**: The codebase adheres to best practices in Android development with Kotlin, emphasizing clear and meaningful naming conventions.

## Other enhancements
- **Comprehensive Test Coverage**: Extended testing to cover all aspects of the app.
- **UI/UX Enhancements**: Updated app styling using material design and improve map design for better user experience.
- **Reminders Management**: Implemented features to edit and delete reminders and their corresponding geofence requests.

---
For detailed setup and usage instructions, please refer to the individual documentation files in the project repository.
The app was originaly meant for a maximum API of 29 but I converted it into a API 33+ app. It took some time but solved over 200 errors in the process.

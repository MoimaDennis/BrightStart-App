# BrightStart Android App

BrightStart is an Android app for a non-profit education initiative. It lets staff manage learners, volunteers, programmes, sessions, attendance, performance and donations from a phone. It uses the same API and database as the BrightStart website, so both share one login and the same data.

**Module:** XISD6329 (IIE) · **Package:** `com.brightstart.app`

## Video demonstration
[https://drive.google.com/file/d/1hJ7GG7SWXCtkk_FnBxRHucfxJfoqxl_l/view?usp=sharing]

## Links
| Item | Link |
| --- | --- |
| Website repository (GitHub) | https://github.com/MoimaDennis/[website-repository] |
| Website repository (Azure) | https://dev.azure.com/ST104519320228/BrightStart-Part2/_git/BrightStart-Web |
| App repository (Azure) | https://dev.azure.com/ST104519320228/BrightStart-Part2/_git/BrightStart-Part2 |
| Live website (when ngrok is running) | https://cartoon-decency-unblessed.ngrok-free.dev/brightstart-web/ |

## Problem and purpose
BrightStart tracked learners, volunteers, sessions and donations on paper and spreadsheets, so records were hard to find and not secure. The app and website give staff one secure system that works in a browser and on a phone.

## Features
- Register and login with input validation (password of 8 or more characters, with a letter and a digit)
- Google single sign-on (Firebase Authentication)
- Admin approval of new users, and role-based access
- Admin screens: approve or reject users, add students
- Dashboard statistics and banner, matching the website
- Modules: students, programmes, volunteers, sessions, enrolments, attendance, performance, donations
- Settings menu (profile and light/dark theme) and a help screen
- Clear error messages, for example when the server is unreachable

## Tech stack
| Layer | Technology |
| --- | --- |
| App | Kotlin, Android Views, coroutines |
| Networking | Retrofit, Gson, OkHttp (cookie jar for the PHP session) |
| Sign-in | Firebase Authentication, Google Sign-In |
| Back end | PHP 8 JSON API and MySQL (see the website repository) |
| Testing and CI | JUnit unit tests, GitHub Actions, Azure Pipelines |

## How it works
```
Android app  --HTTPS/JSON-->  PHP API (/brightstart-web/api/)  -->  MySQL
Website      --HTTPS/JSON-->  (same API and database)
```

## Run the app
**Requirements:** Android Studio, an Android phone with USB debugging (or an emulator), XAMPP, ngrok.

1. Start the server (see the website repository):
   - Open XAMPP and start **Apache** and **MySQL**.
   - Import `db/schema.sql` in phpMyAdmin.
   - Run `ngrok http --url=cartoon-decency-unblessed.ngrok-free.dev 80`
2. Clone this repository and open it in Android Studio.
3. Set the API address in `BsApi.kt`:
   ```kotlin
   const val BASE_URL = "https://cartoon-decency-unblessed.ngrok-free.dev/brightstart-web/api/"
   ```
4. Add your own `google-services.json` (Firebase project) to the `app` folder.
5. Click **Sync Project**, select your phone, then click **Run**.

## Run the tests
```
gradlew.bat test
```
The report is at `app/build/reports/tests/testDebugUnitTest/index.html`. Tests also run automatically in GitHub Actions (`.github/workflows/android.yml`) and Azure Pipelines (`azure-pipelines.yml`).

## Build a release APK
Android Studio → **Build** → **Generate Signed Bundle / APK** → create a keystore → choose **release**. Keep the keystore file and passwords private and out of Git.

## Google Play readiness
- Package name: `com.brightstart.app`
- Signed release build: [state whether done]
- Play Console status: [state what is true, e.g. internal testing / not yet published]

## Screenshots
| Login | Dashboard | Admin approvals |
| --- | --- | --- |
| [image] | [image] | [image] |

Add images to a `docs/` folder and link them like `![Login](docs/login.png)`.

## Team
| Name | Student number |
| --- | --- |
| Dennis Moima | ST10451932 |
| Kgothatso Magatsela | ST10448288 |
| Keabetswe Modiba | ST10438507 |
| Chantal Mafa | ST10444319 |

## Security notes
Passwords are hashed with bcrypt on the server. Secrets, keystores and access tokens are never stored in this repository.

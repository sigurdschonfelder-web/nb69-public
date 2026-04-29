# NB69

NB69 is a full-stack web application for managing chores and event planning using a simple, interactive interface.

## Project Overview

- **Frontend:** React application built with Vite.
- **Backend:** Spring Boot Java service.
- **Database:** Hosted on Neon.
- **Deployments:** Frontend on Vercel, backend on Render.
- **Domain:** `nb69.no` is connected to the deployed application.

## What it Does

The app provides functionality for:

- Viewing and managing chores.
- Working with prikker (poll-style scheduling entries).
- Supporting administrative actions through a dedicated admin interface.

## Repository Structure

- `frontend/` - React source code, styling, API clients, and pages.
- `backend/` - Spring Boot service with controllers, models, and configuration.
- `backend/src/main/resources/` - application properties and static resources.

## Tech Stack

- Frontend: React, Vite, Tailwind CSS, JavaScript/JSX
- Backend: Java, Spring Boot, Maven
- Database: PostgreSQL on Neon
- Hosting: Vercel for frontend, Render for backend
- Domain: `nb69.no`

## Run Locally

### Frontend

1. Open a terminal in `frontend/`
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the development server:
   ```bash
   npm run dev
   ```
4. Open the local URL shown in the terminal.

### Backend

1. Open a terminal in `backend/NB69/`
2. Build and run with Maven:
   ```bash
   ./mvnw spring-boot:run
   ```
3. The backend starts on the port configured in `application.properties`.

## Deployment Notes

- The frontend is deployed on Vercel and serves the React application.
- The backend is deployed on Render and exposes the REST API used by the frontend.
- The PostgreSQL database is hosted on Neon and stores application data.
- The public domain `nb69.no` is connected to the deployed services.

## Useful Links

- Frontend deployment: Vercel
- Backend deployment: Render
- Database host: Neon
- Domain: `https://nb69.no`

## Contact

For updates or issues, check the repository and deployment settings for the latest configuration.

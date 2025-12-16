# Moodi – ASD Emotional Regulation Mobile App (Child & Parent)

This repository contains **Moodi**, an Android mobile application developed as part of the ASD Emotional Support Project, designed for **high-functioning autistic children** and their **parents**.

Moodi enables children to express and reflect on their emotions in a **structured, visual, and pressure-free** way, while allowing parents to gain insight into emotional patterns and support emotional regulation in daily life.

---

## Project Context

The ASD Emotional Support Project serves **three user roles**:

* **Child** – high-functioning autistic child (ages 10+)
* **Parent**
* **Therapist**

This repository focuses **only on the mobile application**, which supports:

* Child emotional logging and self-expression
* Parent monitoring, prompts, and emotional insight

The **therapist-facing web interface** is implemented in a separate repository.

---

## Target Users

### Child

A high-functioning autistic child who can use the app independently and experiences difficulty identifying or expressing emotions, especially in social or overstimulating situations.

### Parent

A caregiver who supports the child’s emotional development, routines, and regulation, and uses Moodi to better understand the child’s emotional experiences.

---

## Core Purpose

Moodi focuses on:

* Helping children **identify and express emotions**, even when verbal explanation is difficult
* Supporting **emotional regulation** through reflection and awareness
* Allowing parents to **observe emotional patterns** and respond appropriately
* Maintaining clear boundaries between user roles and permissions

The app does **not** attempt to interpret social situations or diagnose behavior.
It centers on how the child **experiences and feels** events.

---

## Key Mobile App Capabilities

### Child-Facing Features

* Select an existing situation or enter a new one
* Choose an emotion using visual icons (emoji + label)
* Rate emotional intensity using a slider
* Optionally:

  * Add a short text or voice note
  * Take a photo to reflect facial expression
* Review past emotional entries in a simplified timeline
* Receive reminders to log emotions

### Parent-Facing Features

* View the child’s emotional logs
* Schedule tasks and reminders for emotional check-ins
* Review therapist notes attached to emotional entries
* Observe emotional trends over time

---

## Access & Ethics

* Parents and therapists have access to the child’s emotional logs, in accordance with ethical and therapeutic guidelines
* Moodi respects **privacy, autonomy, and emotional safety**
* The system is designed as a **supportive tool**, not a replacement for professional therapy

---

## Technology Stack

* **Platform:** Android
* **Language:** Java
* **Backend Services:**

  * Firebase Authentication
  * Firestore Database
  * Firebase Storage

---

## Current Status

* Core screens and emotional logging flows are under active development
* Role-based behavior is implemented at the design and logic level
* UI and UX are still being refined
* Some features are partially implemented or pending testing
* Additional iterations will expand analytics and integrations

This README reflects the **current state of the Moodi mobile application** and will be updated as development progresses.

---

## Related Repository

Therapist Web Interface:
[https://github.com/bluey99/ASD-Therapist-Web](https://github.com/bluey99/ASD-Project.git)


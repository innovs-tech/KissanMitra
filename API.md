# KissanMitra API Documentation

## Overview

This document provides detailed API documentation for the KissanMitra platform, including authentication, user management, device onboarding, and administrative functions.

## Base URL

```
http://localhost:8080/api/v1
```

## Authentication

### Login

**Endpoint:** `POST /auth/login`

**Description:** Authenticate user and return JWT token.

**Request Body:**

```json
{
  "phone": "string",
  "otp": "string"
}
```

**Response:**

```json
{
  "token": "string",
  "user": {
    "id": "string",
    "phone": "string",
    "role": "string"
  }
}
```

### Register

**Endpoint:** `POST /auth/register`

**Description:** Register a new user.

**Request Body:**

```json
{
  "phone": "string",
  "name": "string"
}
```

**Response:**

```json
{
  "message": "OTP sent to phone"
}
```

### Verify OTP

**Endpoint:** `POST /auth/verify`

**Description:** Verify OTP for registration/login.

**Request Body:**

```json
{
  "phone": "string",
  "otp": "string"
}
```

## User Management

### Get Profile

**Endpoint:** `GET /users/profile`

**Description:** Get current user profile.

**Headers:** `Authorization: Bearer <token>`

**Response:**

```json
{
  "id": "string",
  "phone": "string",
  "name": "string",
  "role": "string"
}
```

### Update Profile

**Endpoint:** `PUT /users/profile`

**Description:** Update user profile.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**

```json
{
  "name": "string"
}
```

## Device Onboarding

### Create Device

**Endpoint:** `POST /admin/devices`

**Description:** Onboard a new device (Admin only).

**Headers:** `Authorization: Bearer <token>`

**Request Body:**

```json
{
  "type": "TRACTOR",
  "manufacturer": "MAHINDRA",
  "model": "string",
  "location": {
    "type": "Point",
    "coordinates": [longitude, latitude]
  }
}
```

**Response:**

```json
{
  "id": "string",
  "status": "AVAILABLE"
}
```

### Get Devices

**Endpoint:** `GET /admin/devices`

**Description:** List all devices (Admin only).

**Headers:** `Authorization: Bearer <token>`

**Query Parameters:**

- `page`: integer
- `size`: integer

**Response:**

```json
{
  "devices": [
    {
      "id": "string",
      "type": "string",
      "status": "string"
    }
  ],
  "total": 0
}
```

## Order Management

### Create Order

**Endpoint:** `POST /orders`

**Description:** Create a new rental order.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**

```json
{
  "deviceId": "string",
  "type": "RENT",
  "startDate": "2023-01-01",
  "endDate": "2023-01-02"
}
```

**Response:**

```json
{
  "id": "string",
  "status": "INTEREST_RAISED"
}
```

### Get Orders

**Endpoint:** `GET /orders`

**Description:** Get user's orders.

**Headers:** `Authorization: Bearer <token>`

**Response:**

```json
{
  "orders": [
    {
      "id": "string",
      "status": "string",
      "device": {
        "id": "string",
        "type": "string"
      }
    }
  ]
}
```

## Lease Management

### Create Lease

**Endpoint:** `POST /leases`

**Description:** Create a lease agreement.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**

```json
{
  "deviceId": "string",
  "farmerId": "string",
  "terms": "string"
}
```

## Push Notifications

### Register Token

**Endpoint:** `POST /push-tokens`

**Description:** Register device token for push notifications.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**

```json
{
  "token": "string",
  "platform": "IOS|ANDROID"
}
```

## Discovery

### Search Devices

**Endpoint:** `GET /discovery/devices`

**Description:** Search available devices by location and type.

**Query Parameters:**

- `lat`: double
- `lng`: double
- `type`: string
- `radius`: integer

**Response:**

```json
{
  "devices": [
    {
      "id": "string",
      "distance": 0.0
    }
  ]
}
```

## Error Responses

All endpoints may return the following error responses:

**400 Bad Request:**

```json
{
  "error": "Invalid request",
  "message": "Detailed error message"
}
```

**401 Unauthorized:**

```json
{
  "error": "Unauthorized",
  "message": "Invalid or missing token"
}
```

**403 Forbidden:**

```json
{
  "error": "Forbidden",
  "message": "Insufficient permissions"
}
```

**404 Not Found:**

```json
{
  "error": "Not found",
  "message": "Resource not found"
}
```

**500 Internal Server Error:**

```json
{
  "error": "Internal server error",
  "message": "Something went wrong"
}
```

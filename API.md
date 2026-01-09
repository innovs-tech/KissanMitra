# KissanMitra API Documentation

## Overview

This document provides detailed API documentation for the KissanMitra platform, including authentication, user management, device onboarding, and administrative functions.

## Base URL

```
http://localhost:8080/api/v1
```

## Authentication

### Send OTP

**Endpoint:** `POST /auth/send-otp`

**Description:** Send OTP to the given phone number for authentication.

**Query Parameters:**

- `phoneNumber`: string (required) - Phone number to send OTP to

**Response:**

```json
{
  "success": true,
  "requestId": "string",
  "data": "OTP sent successfully"
}
```

### Verify OTP

**Endpoint:** `POST /auth/verify-otp`

**Description:** Verify OTP and return JWT token with user authentication.

**Request Body:**

```json
{
  "phoneNumber": "string",
  "otp": "string"
}
```

**Response:**

```json
{
  "success": true,
  "requestId": "string",
  "data": {
    "token": "string",
    "user": {
      "id": "string",
      "phone": "string",
      "name": "string",
      "roles": ["string"],
      "activeRole": "string"
    }
  }
}
```

### Select Active Role

**Endpoint:** `POST /auth/session/role`

**Description:** Select active role for the current session and get new JWT token.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**

```json
{
  "activeRole": "string"
}
```

**Response:**

```json
{
  "success": true,
  "requestId": "string",
  "data": "new-jwt-token-string"
}
```

### Get Current User

**Endpoint:** `GET /auth/me`

**Description:** Get current authenticated user details.

**Headers:** `Authorization: Bearer <token>`

**Response:**

```json
{
  "success": true,
  "requestId": "string",
  "data": {
    "id": "string",
    "phone": "string",
    "name": "string",
    "roles": ["string"],
    "activeRole": "string"
  }
}
```

## User Management

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

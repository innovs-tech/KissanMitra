# Order Flow Testing Guide

## Overview

This document provides a comprehensive guide for testing the Order Management flow in KissanMitra. Orders represent user intent to lease or rent equipment, and follow a deterministic state machine with role-based authorization.

## Base URL

```
http://localhost:8080/api/v1
```

## Order Types

### LEASE Order
- **Requester:** VLE (Village Level Entrepreneur)
- **Handler:** ADMIN
- **Purpose:** VLE requests to lease equipment from Company
- **Prerequisite:** Device must NOT be leased (`currentLeaseId == null`)
- **Outcome:** Leads to Lease creation when ACCEPTED

### RENT Order
- **Requester:** FARMER
- **Handler:** VLE (device owner)
- **Purpose:** FARMER requests to rent equipment from VLE
- **Prerequisite:** Device must be leased (`currentLeaseId != null`)
- **Outcome:** Direct rental agreement, no lease creation

## Order State Machine

```
DRAFT → INTEREST_RAISED → UNDER_REVIEW → ACCEPTED → PICKUP_SCHEDULED → ACTIVE → COMPLETED → CLOSED
                                    ↓
                                 REJECTED (terminal)
                                    ↑
                              CANCELLED (terminal)
```

### Valid State Transitions

| From State | To States |
|------------|-----------|
| `DRAFT` | `INTEREST_RAISED` |
| `INTEREST_RAISED` | `UNDER_REVIEW`, `ACCEPTED`, `REJECTED`, `CANCELLED` |
| `UNDER_REVIEW` | `ACCEPTED`, `REJECTED` |
| `ACCEPTED` | `PICKUP_SCHEDULED` |
| `PICKUP_SCHEDULED` | `ACTIVE` |
| `ACTIVE` | `COMPLETED` |
| `COMPLETED` | `CLOSED` |

**Terminal States:** `CLOSED`, `REJECTED`, `CANCELLED` (cannot transition from these)

## Prerequisites

Before testing order flow, ensure:

1. **Users Created:**
   - ADMIN user (with ADMIN role)
   - VLE user (with VLE role + VLE Profile)
   - FARMER user (with FARMER role)

2. **Device Setup:**
   - Device with status `LIVE`
   - Device has active pricing rules
   - For RENT flow: Device must be leased (create lease first)

3. **Authentication:**
   - Get OTP and verify for each user
   - Set active role for each user session

---

## Authentication Setup

### Step 1: Send OTP

**Endpoint:** `POST /auth/send-otp`

**Request:**
```json
{
  "phoneNumber": "+919876543210"
}
```

**Response:**
```json
{
  "success": true,
  "message": "OTP sent successfully.",
  "data": null
}
```

### Step 2: Verify OTP

**Endpoint:** `POST /auth/verify-otp`

**Request:**
```json
{
  "phoneNumber": "+919876543210",
  "otp": "123456"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Authentication successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": "user-id-123",
      "phone": "+919876543210",
      "roles": ["VLE"],
      "activeRole": null
    }
  }
}
```

### Step 3: Set Active Role

**Endpoint:** `POST /auth/session/role`

**Headers:** `Authorization: Bearer <token>`

**Request:**
```json
{
  "activeRole": "VLE"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Active role updated",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": "user-id-123",
      "phone": "+919876543210",
      "roles": ["VLE"],
      "activeRole": "VLE"
    }
  }
}
```

**Important:** Use the NEW token returned from this endpoint for subsequent API calls.

---

## Flow 1: LEASE Order (VLE → Admin)

### Step 1: Create LEASE Order

**Endpoint:** `POST /orders`

**Headers:** `Authorization: Bearer <vle_token>`

**Request:**
```json
{
  "deviceId": "device-id-123",
  "requestedHours": 10.0,
  "requestedAcres": 5.0,
  "startDate": "2024-01-15",
  "endDate": "2024-01-20",
  "note": "Need equipment for farming season"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Operation successful",
  "data": {
    "id": "order-id-123",
    "orderType": "LEASE",
    "status": "INTEREST_RAISED",
    "deviceId": "device-id-123",
    "requestedBy": "vle-user-id",
    "handledBy": {
      "type": "ADMIN",
      "id": "admin"
    },
    "requestedHours": 10.0,
    "requestedAcres": 5.0,
    "startDate": "2024-01-15",
    "endDate": "2024-01-20",
    "phone": "+919876543210",
    "name": "VLE Name",
    "note": "Need equipment for farming season"
  },
  "correlationId": "req-id-123",
  "timestamp": "2024-01-08T12:00:00Z"
}
```

**Notifications Sent:**
- **Admin:** "New LEASE order #order-id created by VLE. Device: device-id, Requested: 10.0 hours / 5.0 acres. Please review."
- **VLE:** "Your LEASE order #order-id has been submitted successfully. Device: device-id, Requested: 10.0 hours / 5.0 acres. We'll review it shortly."

**Validation:**
- Device must exist and be `LIVE`
- Device must NOT be leased (`currentLeaseId == null`)
- User must have VLE role active
- `startDate` and `endDate` are required

---

### Step 2: View LEASE Orders (Admin)

**Endpoint:** `GET /admin/orders/lease`

**Headers:** `Authorization: Bearer <admin_token>`

**Response:**
```json
{
  "success": true,
  "message": "Operation successful",
  "data": [
    {
      "id": "order-id-123",
      "orderType": "LEASE",
      "status": "INTEREST_RAISED",
      "deviceId": "device-id-123",
      "requestedBy": "vle-user-id",
      "handledBy": {
        "type": "ADMIN",
        "id": "admin"
      },
      "requestedHours": 10.0,
      "requestedAcres": 5.0,
      "startDate": "2024-01-15",
      "endDate": "2024-01-20"
    }
  ]
}
```

---

### Step 3: Update Order Status (Admin)

**Option A: Move to UNDER_REVIEW**

**Endpoint:** `PATCH /orders/{orderId}/status`

**Headers:** `Authorization: Bearer <admin_token>`

**Request:**
```json
{
  "toState": "UNDER_REVIEW",
  "note": "Reviewing the request"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Operation successful",
  "data": {
    "id": "order-id-123",
    "status": "UNDER_REVIEW",
    ...
  }
}
```

**Notifications Sent:**
- **VLE (Requester):** "Your order #order-id status updated: INTEREST_RAISED → UNDER_REVIEW"
- **Admin (Handler):** "Order #order-id you're handling status updated: INTEREST_RAISED → UNDER_REVIEW"

---

**Option B: Accept Order**

**Endpoint:** `PATCH /orders/{orderId}/status`

**Headers:** `Authorization: Bearer <admin_token>`

**Request:**
```json
{
  "toState": "ACCEPTED",
  "note": "Order approved"
}
```

**Notifications Sent:**
- **VLE (Requester):** "Your order #order-id status updated: INTEREST_RAISED → ACCEPTED"
- **Admin (Handler):** "Order #order-id you're handling status updated: INTEREST_RAISED → ACCEPTED"

**Important:** Once order is `ACCEPTED`, device becomes unavailable in discovery.

---

**Option C: Reject Order**

**Endpoint:** `POST /admin/orders/{orderId}/reject`

**Headers:** `Authorization: Bearer <admin_token>`

**Request:**
```json
{
  "note": "Equipment not available"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Operation successful",
  "data": {
    "id": "order-id-123",
    "status": "REJECTED",
    "note": "Equipment not available"
  }
}
```

**Notifications Sent:**
- **VLE (Requester):** "Your order #order-id has been rejected. Reason: Equipment not available"

---

### Step 4: Continue Status Progression (Admin)

**Move to PICKUP_SCHEDULED:**

**Endpoint:** `PATCH /orders/{orderId}/status`

**Request:**
```json
{
  "toState": "PICKUP_SCHEDULED",
  "note": "Pickup scheduled for Jan 15"
}
```

**Move to ACTIVE:**

**Request:**
```json
{
  "toState": "ACTIVE",
  "note": "Equipment handed over"
}
```

**Move to COMPLETED:**

**Request:**
```json
{
  "toState": "COMPLETED",
  "note": "Usage period completed"
}
```

**Move to CLOSED:**

**Request:**
```json
{
  "toState": "CLOSED",
  "note": "Order closed"
}
```

---

### Step 5: Create Lease from ACCEPTED Order (Admin)

**Endpoint:** `POST /admin/leases`

**Headers:** `Authorization: Bearer <admin_token>`

**Content-Type:** `multipart/form-data`

**Form Data:**
- `request` (JSON part):
```json
{
  "orderId": "order-id-123",
  "depositAmount": 5000.0,
  "operators": [
    {
      "operatorId": "operator-id-1",
      "role": "PRIMARY"
    }
  ],
  "notes": "Lease agreement signed"
}
```

- `attachmentFiles` (optional): Array of files (PDF, JPG, PNG)

**Response:**
```json
{
  "success": true,
  "message": "Operation successful",
  "data": {
    "id": "lease-id-123",
    "deviceId": "device-id-123",
    "vleId": "vle-profile-id",
    "status": "ACTIVE",
    "estimatedPrice": 15000.0,
    "depositAmount": 5000.0,
    "startDate": "2024-01-15",
    "endDate": "2024-01-20",
    "operators": [...],
    "attachments": [...]
  }
}
```

**Notifications Sent:**
- **VLE:** "Your lease #lease-id has been created successfully! Device: device-id, Start: 2024-01-15, End: 2024-01-20. You can now start operations."
- **Admin:** "Lease #lease-id created successfully for VLE. Device: device-id, Start: 2024-01-15, End: 2024-01-20"

**Important:** After lease creation, `device.currentLeaseId` is set, making device available for RENT orders.

---

## Flow 2: RENT Order (FARMER → VLE)

### Step 1: Create RENT Order

**Endpoint:** `POST /orders`

**Headers:** `Authorization: Bearer <farmer_token>`

**Prerequisite:** Device must be leased (have `currentLeaseId`)

**Request:**
```json
{
  "deviceId": "device-id-123",
  "requestedHours": 8.0,
  "requestedAcres": 3.0,
  "startDate": "2024-01-25",
  "endDate": "2024-01-27",
  "note": "Need for my farm"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Operation successful",
  "data": {
    "id": "order-id-456",
    "orderType": "RENT",
    "status": "INTEREST_RAISED",
    "deviceId": "device-id-123",
    "requestedBy": "farmer-user-id",
    "handledBy": {
      "type": "VLE",
      "id": "vle-profile-id"
    },
    "requestedHours": 8.0,
    "requestedAcres": 3.0,
    "startDate": "2024-01-25",
    "endDate": "2024-01-27",
    "phone": "+919876543211",
    "name": "Farmer Name"
  }
}
```

**Notifications Sent:**
- **VLE (Handler):** "New RENT order #order-id from Farmer. Device: device-id, Requested: 8.0 hours / 3.0 acres. Please review."
- **FARMER (Requester):** "Your RENT order #order-id has been submitted successfully. Device: device-id, Requested: 8.0 hours / 3.0 acres. VLE will review it shortly."

**Validation:**
- Device must exist and be `LIVE`
- Device must be leased (`currentLeaseId != null`)
- User must have FARMER role active
- `startDate` and `endDate` are required

---

### Step 2: View RENT Orders (VLE)

**Endpoint:** `GET /vles/orders/rent`

**Headers:** `Authorization: Bearer <vle_token>`

**Response:**
```json
{
  "success": true,
  "message": "Operation successful",
  "data": [
    {
      "id": "order-id-456",
      "orderType": "RENT",
      "status": "INTEREST_RAISED",
      "deviceId": "device-id-123",
      "requestedBy": "farmer-user-id",
      "handledBy": {
        "type": "VLE",
        "id": "vle-profile-id"
      },
      "requestedHours": 8.0,
      "requestedAcres": 3.0,
      "startDate": "2024-01-25",
      "endDate": "2024-01-27"
    }
  ]
}
```

---

### Step 3: Update Order Status (VLE)

**Accept Order:**

**Endpoint:** `PATCH /orders/{orderId}/status`

**Headers:** `Authorization: Bearer <vle_token>`

**Request:**
```json
{
  "toState": "ACCEPTED",
  "note": "Accepted your request"
}
```

**Notifications Sent:**
- **FARMER (Requester):** "Your order #order-id status updated: INTEREST_RAISED → ACCEPTED"
- **VLE (Handler):** "Order #order-id you're handling status updated: INTEREST_RAISED → ACCEPTED"

---

**Reject Order:**

**Endpoint:** `POST /vles/orders/{orderId}/reject`

**Headers:** `Authorization: Bearer <vle_token>`

**Request:**
```json
{
  "note": "Not available on requested dates"
}
```

**Notifications Sent:**
- **FARMER (Requester):** "Your order #order-id has been rejected. Reason: Not available on requested dates"

---

## Common Operations

### Get My Orders

**Endpoint:** `GET /orders`

**Headers:** `Authorization: Bearer <token>`

**Description:** Get all orders created by the current user (any status)

**Response:**
```json
{
  "success": true,
  "message": "Operation successful",
  "data": [
    {
      "id": "order-id-123",
      "orderType": "LEASE",
      "status": "INTEREST_RAISED",
      "deviceId": "device-id-123",
      "requestedBy": "user-id",
      "handledBy": {...},
      "requestedHours": 10.0,
      "requestedAcres": 5.0,
      "startDate": "2024-01-15",
      "endDate": "2024-01-20"
    }
  ]
}
```

---

### Get Order by ID

**Endpoint:** `GET /orders/{orderId}`

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "success": true,
  "message": "Operation successful",
  "data": {
    "id": "order-id-123",
    "orderType": "LEASE",
    "status": "INTEREST_RAISED",
    "deviceId": "device-id-123",
    "requestedBy": "user-id",
    "handledBy": {
      "type": "ADMIN",
      "id": "admin"
    },
    "requestedHours": 10.0,
    "requestedAcres": 5.0,
    "startDate": "2024-01-15",
    "endDate": "2024-01-20",
    "phone": "+919876543210",
    "name": "User Name",
    "note": "Order note"
  }
}
```

---

### Cancel Order (Requester Only)

**Endpoint:** `POST /orders/{orderId}/cancel`

**Headers:** `Authorization: Bearer <requester_token>`

**Description:** Cancel an order. Only the requester can cancel their own order. Only works if status is `INTEREST_RAISED`.

**Request:**
```json
{
  "note": "Changed my mind"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Operation successful",
  "data": {
    "id": "order-id-123",
    "status": "CANCELLED",
    "note": "Changed my mind"
  }
}
```

**Notifications Sent:**
- **Handler (Admin/VLE):** "Order #order-id has been cancelled by the requester."

**Validation:**
- Only requester can cancel
- Can only cancel if status is `INTEREST_RAISED`

---

## Authorization Rules

### Who Can Do What

| Action | LEASE Order | RENT Order |
|--------|-------------|------------|
| **Create Order** | VLE (requester) | FARMER (requester) |
| **View Orders** | Admin (all LEASE), VLE (own orders) | VLE (assigned RENT), FARMER (own orders) |
| **Update Status** | Admin (handler) | VLE (handler) |
| **Reject Order** | Admin (handler) | VLE (handler) |
| **Cancel Order** | VLE (requester) | FARMER (requester) |

---

## SMS Notifications

All order events trigger SMS notifications via Twilio. Phone numbers are automatically formatted with `+91` prefix if missing.

### Notification Recipients

| Event | LEASE Order | RENT Order |
|-------|-------------|------------|
| **Order Created** | Admin + VLE (requester) | VLE (handler) + FARMER (requester) |
| **Status Updated** | Admin (handler) + VLE (requester) | VLE (handler) + FARMER (requester) |
| **Order Cancelled** | Admin (handler) | VLE (handler) |
| **Order Rejected** | VLE (requester) | FARMER (requester) |

### Message Examples

**Order Created (LEASE) - Admin:**
```
New LEASE order #abc12345 created by VLE. Device: device-id, Requested: 10.0 hours / 5.0 acres. Please review.
```

**Order Created (LEASE) - VLE:**
```
Your LEASE order #abc12345 has been submitted successfully. Device: device-id, Requested: 10.0 hours / 5.0 acres. We'll review it shortly.
```

**Order Status Updated - Requester:**
```
Your order #abc12345 status updated: INTEREST_RAISED → ACCEPTED
```

**Order Status Updated - Handler:**
```
Order #abc12345 you're handling status updated: INTEREST_RAISED → ACCEPTED
```

**Order Rejected - Requester:**
```
Your order #abc12345 has been rejected. Reason: Equipment not available
```

**Order Cancelled - Handler:**
```
Order #abc12345 has been cancelled by the requester.
```

---

## Testing Checklist

### LEASE Order Flow

- [ ] **Setup:**
  - [ ] Create ADMIN user
  - [ ] Create VLE user with VLE Profile
  - [ ] Create device with `LIVE` status (not leased)
  - [ ] Set up pricing rules for device

- [ ] **Order Creation:**
  - [ ] Login as VLE, set active role to VLE
  - [ ] Create LEASE order
  - [ ] Verify order created with `INTEREST_RAISED` status
  - [ ] Verify `orderType` is `LEASE`
  - [ ] Verify `handledBy.type` is `ADMIN`
  - [ ] Check SMS notifications sent to Admin and VLE

- [ ] **Order Management (Admin):**
  - [ ] Login as Admin
  - [ ] View all LEASE orders
  - [ ] Update status: `INTEREST_RAISED` → `UNDER_REVIEW`
  - [ ] Update status: `UNDER_REVIEW` → `ACCEPTED`
  - [ ] Check SMS notifications for each status change
  - [ ] Verify device is no longer in discovery

- [ ] **Lease Creation:**
  - [ ] Create lease from ACCEPTED order
  - [ ] Verify lease created successfully
  - [ ] Verify `device.currentLeaseId` is set
  - [ ] Check SMS notifications to VLE and Admin

- [ ] **Status Progression:**
  - [ ] Update status: `ACCEPTED` → `PICKUP_SCHEDULED`
  - [ ] Update status: `PICKUP_SCHEDULED` → `ACTIVE`
  - [ ] Update status: `ACTIVE` → `COMPLETED`
  - [ ] Update status: `COMPLETED` → `CLOSED`

- [ ] **Rejection Flow:**
  - [ ] Create new LEASE order
  - [ ] Admin rejects order
  - [ ] Verify status is `REJECTED`
  - [ ] Check SMS notification to VLE
  - [ ] Verify order cannot be updated further

- [ ] **Cancellation Flow:**
  - [ ] Create new LEASE order
  - [ ] VLE cancels order (as requester)
  - [ ] Verify status is `CANCELLED`
  - [ ] Check SMS notification to Admin
  - [ ] Verify order cannot be updated further

---

### RENT Order Flow

- [ ] **Setup:**
  - [ ] Create FARMER user
  - [ ] Ensure device is leased (create lease first)
  - [ ] Verify `device.currentLeaseId` is set

- [ ] **Order Creation:**
  - [ ] Login as FARMER, set active role to FARMER
  - [ ] Create RENT order
  - [ ] Verify order created with `INTEREST_RAISED` status
  - [ ] Verify `orderType` is `RENT`
  - [ ] Verify `handledBy.type` is `VLE`
  - [ ] Verify `handledBy.id` matches VLE profile ID
  - [ ] Check SMS notifications sent to VLE and FARMER

- [ ] **Order Management (VLE):**
  - [ ] Login as VLE (device owner)
  - [ ] View all RENT orders assigned to this VLE
  - [ ] Update status: `INTEREST_RAISED` → `ACCEPTED`
  - [ ] Check SMS notifications for status change
  - [ ] Continue status progression through lifecycle

- [ ] **Rejection Flow:**
  - [ ] Create new RENT order
  - [ ] VLE rejects order
  - [ ] Verify status is `REJECTED`
  - [ ] Check SMS notification to FARMER

- [ ] **Cancellation Flow:**
  - [ ] Create new RENT order
  - [ ] FARMER cancels order (as requester)
  - [ ] Verify status is `CANCELLED`
  - [ ] Check SMS notification to VLE

---

### Edge Cases & Error Scenarios

- [ ] **Invalid Order Creation:**
  - [ ] Try to create LEASE order for leased device (should fail)
  - [ ] Try to create RENT order for unleased device (should fail)
  - [ ] Try to create order without authentication (should fail)
  - [ ] Try to create order with wrong active role (should fail)
  - [ ] Try to create order without `startDate`/`endDate` (should fail)

- [ ] **Invalid Status Transitions:**
  - [ ] Try invalid transition (e.g., `INTEREST_RAISED` → `ACTIVE`) (should fail)
  - [ ] Try to transition from terminal state (should fail)
  - [ ] Try to update status as non-handler (should fail)

- [ ] **Invalid Authorization:**
  - [ ] Try to reject LEASE order as VLE (should fail - only Admin can reject)
  - [ ] Try to reject RENT order as Admin (should fail - only VLE can reject)
  - [ ] Try to cancel order as non-requester (should fail)
  - [ ] Try to cancel order in `UNDER_REVIEW` (should fail - only `INTEREST_RAISED`)

- [ ] **Discovery Visibility:**
  - [ ] Verify device with `ACCEPTED` order is not in discovery
  - [ ] Verify device with `ACTIVE` order is not in discovery
  - [ ] Verify device with `COMPLETED` order is not in discovery
  - [ ] Verify device with `CLOSED` order appears in discovery again

---

## Complete Test Flow Example

### Scenario: End-to-End LEASE Order

1. **Setup:**
   ```bash
   # Create device (Admin)
   POST /api/v1/admin/devices/onboard/step1
   # ... complete onboarding
   
   # Create VLE Profile (Admin)
   POST /api/v1/admin/vles
   ```

2. **VLE Creates Order:**
   ```bash
   # Login as VLE
   POST /api/v1/auth/verify-otp
   POST /api/v1/auth/session/role (activeRole: VLE)
   
   # Create LEASE order
   POST /api/v1/orders
   {
     "deviceId": "device-123",
     "requestedHours": 10.0,
     "requestedAcres": 5.0,
     "startDate": "2024-01-15",
     "endDate": "2024-01-20"
   }
   # Response: order-id-123, status: INTEREST_RAISED
   ```

3. **Admin Reviews:**
   ```bash
   # Login as Admin
   POST /api/v1/auth/verify-otp
   POST /api/v1/auth/session/role (activeRole: ADMIN)
   
   # View LEASE orders
   GET /api/v1/admin/orders/lease
   
   # Accept order
   PATCH /api/v1/orders/order-id-123/status
   {
     "toState": "ACCEPTED",
     "note": "Approved"
   }
   ```

4. **Admin Creates Lease:**
   ```bash
   POST /api/v1/admin/leases (multipart/form-data)
   # request: { "orderId": "order-id-123", ... }
   # Response: lease-id-456
   ```

5. **Status Progression:**
   ```bash
   PATCH /api/v1/orders/order-id-123/status
   { "toState": "PICKUP_SCHEDULED" }
   
   PATCH /api/v1/orders/order-id-123/status
   { "toState": "ACTIVE" }
   
   PATCH /api/v1/orders/order-id-123/status
   { "toState": "COMPLETED" }
   
   PATCH /api/v1/orders/order-id-123/status
   { "toState": "CLOSED" }
   ```

---

### Scenario: End-to-End RENT Order

1. **Prerequisite: Device Must Be Leased**
   ```bash
   # Complete LEASE order flow first (Steps 1-4 above)
   # Device now has currentLeaseId set
   ```

2. **FARMER Creates Order:**
   ```bash
   # Login as FARMER
   POST /api/v1/auth/verify-otp
   POST /api/v1/auth/session/role (activeRole: FARMER)
   
   # Create RENT order
   POST /api/v1/orders
   {
     "deviceId": "device-123",
     "requestedHours": 8.0,
     "requestedAcres": 3.0,
     "startDate": "2024-01-25",
     "endDate": "2024-01-27"
   }
   # Response: order-id-789, status: INTEREST_RAISED
   ```

3. **VLE Reviews:**
   ```bash
   # Login as VLE (device owner)
   POST /api/v1/auth/verify-otp
   POST /api/v1/auth/session/role (activeRole: VLE)
   
   # View RENT orders
   GET /api/v1/vles/orders/rent
   
   # Accept order
   PATCH /api/v1/orders/order-id-789/status
   {
     "toState": "ACCEPTED",
     "note": "Accepted"
   }
   ```

4. **Status Progression:**
   ```bash
   # Continue through lifecycle as needed
   PATCH /api/v1/orders/order-id-789/status
   { "toState": "PICKUP_SCHEDULED" }
   
   PATCH /api/v1/orders/order-id-789/status
   { "toState": "ACTIVE" }
   
   PATCH /api/v1/orders/order-id-789/status
   { "toState": "COMPLETED" }
   
   PATCH /api/v1/orders/order-id-789/status
   { "toState": "CLOSED" }
   ```

---

## Error Responses

### 400 Bad Request

**Invalid Request Body:**
```json
{
  "success": false,
  "message": "Validation failed",
  "errorDetails": "{deviceId=Device ID is required}",
  "errorCode": "400",
  "correlationId": "req-id-123",
  "timestamp": "2024-01-08T12:00:00Z"
}
```

**Invalid State Transition:**
```json
{
  "success": false,
  "message": "Operation failed",
  "errorDetails": "Invalid state transition from INTEREST_RAISED to ACTIVE",
  "errorCode": "400",
  "correlationId": "req-id-123",
  "timestamp": "2024-01-08T12:00:00Z"
}
```

**Device Not Available:**
```json
{
  "success": false,
  "message": "Operation failed",
  "errorDetails": "Device must be leased to a VLE for RENT orders",
  "errorCode": "400",
  "correlationId": "req-id-123",
  "timestamp": "2024-01-08T12:00:00Z"
}
```

### 401 Unauthorized

```json
{
  "success": false,
  "message": "Invalid or expired token",
  "errorDetails": "Authentication failed",
  "errorCode": "401",
  "correlationId": "req-id-123",
  "timestamp": "2024-01-08T12:00:00Z"
}
```

### 403 Forbidden

```json
{
  "success": false,
  "message": "Access denied",
  "errorDetails": "Only admins can handle LEASE orders",
  "errorCode": "403",
  "correlationId": "req-id-123",
  "timestamp": "2024-01-08T12:00:00Z"
}
```

### 404 Not Found

```json
{
  "success": false,
  "message": "Resource not found",
  "errorDetails": "Order not found",
  "errorCode": "404",
  "correlationId": "req-id-123",
  "timestamp": "2024-01-08T12:00:00Z"
}
```

---

## Notes

1. **Order Type Determination:**
   - Order type is automatically determined by requester role:
     - FARMER role → RENT order
     - VLE role → LEASE order

2. **Handler Assignment:**
   - LEASE orders: Handler is ADMIN (any admin can handle)
   - RENT orders: Handler is VLE (specific VLE who owns the device via lease)

3. **Device Availability:**
   - Devices with orders in `ACCEPTED`, `PICKUP_SCHEDULED`, `ACTIVE`, or `COMPLETED` status are excluded from discovery
   - Only `CLOSED` orders allow device to be discoverable again

4. **Phone Number Format:**
   - Phone numbers are automatically formatted with `+91` prefix if missing
   - SMS notifications use E.164 format

5. **Notifications:**
   - All notifications are sent asynchronously (fire-and-forget)
   - Notifications are role-specific (different messages for different recipients)
   - Notifications are logged even if Twilio is not configured

---

## Quick Reference

### Order Endpoints

| Endpoint | Method | Role | Description |
|----------|--------|------|-------------|
| `/orders` | POST | VLE/FARMER | Create order |
| `/orders` | GET | Any | Get my orders |
| `/orders/{id}` | GET | Any | Get order by ID |
| `/orders/{id}/status` | PATCH | Handler | Update order status |
| `/orders/{id}/cancel` | POST | Requester | Cancel order |
| `/admin/orders/lease` | GET | ADMIN | Get all LEASE orders |
| `/admin/orders/{id}/reject` | POST | ADMIN | Reject LEASE order |
| `/vles/orders/rent` | GET | VLE | Get RENT orders |
| `/vles/orders/{id}/reject` | POST | VLE | Reject RENT order |

### Order Statuses

- `INTEREST_RAISED` - Initial status after creation
- `UNDER_REVIEW` - Being reviewed by handler
- `ACCEPTED` - Approved, equipment not yet handed over
- `PICKUP_SCHEDULED` - Pickup logistics finalized
- `ACTIVE` - Equipment handed over, usage in progress
- `COMPLETED` - Usage period ended
- `CLOSED` - Final terminal state
- `REJECTED` - Rejected by handler (terminal)
- `CANCELLED` - Cancelled by requester (terminal)

---

## Support

For issues or questions, refer to the main API documentation or contact the development team.


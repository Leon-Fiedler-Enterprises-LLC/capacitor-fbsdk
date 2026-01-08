# capacitor-facebook-events

Facebook events tracking in Capacitor applications

## Install

```bash
npm i --save capacitor-facebook-events
npx cap sync
```

## API

<docgen-index>

* [`setAdvertiserTrackingEnabled(...)`](#setadvertisertrackingenabled)
* [`logEvent(...)`](#logevent)
* [`getFBAnonymousID()`](#getfbanonymousid)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### setAdvertiserTrackingEnabled(...)

```typescript
setAdvertiserTrackingEnabled(options: { enabled: boolean; }) => Promise<void>
```

| Param         | Type                               |
| ------------- | ---------------------------------- |
| **`options`** | <code>{ enabled: boolean; }</code> |

--------------------


### logEvent(...)

```typescript
logEvent(options: { event: string; params?: any; }) => Promise<void>
```

| Param         | Type                                          |
| ------------- | --------------------------------------------- |
| **`options`** | <code>{ event: string; params?: any; }</code> |

--------------------


### getFBAnonymousID()

```typescript
getFBAnonymousID() => Promise<{ anonymousID: string; }>
```

**Returns:** <code>Promise&lt;{ anonymousID: string; }&gt;</code>

--------------------

</docgen-api>

## Example Usage

### Getting the Facebook Anonymous ID
To retrieve the Facebook Anonymous ID for attribution:

```typescript
import { FacebookEvents } from 'capacitor-facebook-events';

const { anonymousID } = await FacebookEvents.getFBAnonymousID();
console.log('Facebook Anonymous ID:', anonymousID);
```

### Logging Custom Events
To log custom user action events:

```typescript
import { FacebookEvents } from 'capacitor-facebook-events';

// Log a registration event
FacebookEvents.logEvent({
    event: 'fb_mobile_complete_registration',
    params: {
        registration_method: 'email'
    }
});

// Log a custom user action
FacebookEvents.logEvent({
    event: 'user_started_trial',
    params: {
        plan_type: 'premium'
    }
});
```

### Important Notes
- **Install events** are automatically tracked when the plugin initializes
- **Purchase events** should NOT be logged manually - RevenueCat sends server-to-server notifications to Meta for accurate purchase tracking
- Auto-logging is disabled to prevent duplicate purchase events

For a comprehensive list of standard events, refer to the [Facebook App Events API documentation](https://developers.facebook.com/docs/marketing-api/app-event-api/).

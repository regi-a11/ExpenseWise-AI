# Supabase Edge Functions Setup

## Overview
This project uses Supabase Edge Functions for secure server-side processing of SMS submissions. The system uses RSA encryption to verify that submissions come from legitimate Android app installations.

## Security Architecture
1. **Android App** has the RSA public key (safe to embed)
2. **Edge Function** has the RSA private key (stored as environment secret)
3. Device ID + timestamp are encrypted by the app and verified by the Edge Function
4. No user tracking - device ID is only used for verification, not stored

## Setup Instructions

### 1. Deploy Edge Function

```bash
# Login to Supabase
npx supabase login

# Link to your project
npx supabase link --project-ref YOUR_PROJECT_REF

# Set the private key secret (use PKCS#8 format)
npx supabase secrets set APP_PRIVATE_KEY="$(cat private_key_pkcs8_base64.txt)"

# Deploy the Edge Function
npx supabase functions deploy submit-sms
```

### 2. Alternative: Set Secret via Dashboard
1. Go to Supabase Dashboard
2. Navigate to Project Settings > Edge Functions > Secrets
3. Add new secret:
   - Name: `APP_PRIVATE_KEY`
   - Value: Contents of `private_key_base64.txt`

### 3. Test the Function

```bash
# Test locally
npx supabase functions serve submit-sms

# Test deployed function
curl -X POST https://YOUR_PROJECT_REF.supabase.co/functions/v1/submit-sms \
  -H "Content-Type: application/json" \
  -d '{
    "smsBody": "Test SMS",
    "sender": "TEST",
    "encryptedDeviceData": "BASE64_ENCRYPTED_DATA"
  }'
```

## Key Files
- `functions/submit-sms/index.ts` - Edge Function for SMS submission
- `functions/_shared/cors.ts` - CORS configuration
- `private_key.pem` - RSA private key (DO NOT COMMIT)
- `public_key.pem` - RSA public key (embedded in Android app)

## Security Notes
- **NEVER** commit private keys to version control
- The private key should only exist as a Supabase secret
- Device IDs are never stored in the database
- Timestamps prevent replay attacks (5-minute window)
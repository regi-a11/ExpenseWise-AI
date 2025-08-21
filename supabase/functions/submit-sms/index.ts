import "jsr:@supabase/functions-js/edge-runtime.d.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { corsHeaders } from '../_shared/cors.ts'

interface SubmissionRequest {
  smsBody: string
  sender: string
  encryptedDeviceData: string  // Base64 encoded encrypted data
  parsedData?: {
    amount?: number
    type?: string
    merchant?: string
    category?: string
    reference?: string
    accountLast4?: string
    balance?: number
  }
  userExpected?: {
    amount?: number
    merchant?: string
    category?: string
    type?: string
    isTransaction?: boolean
  }
  additionalNotes?: string
  mode?: 'submit' | 'improve'
}

async function decryptWithPrivateKey(encryptedBase64: string, privateKeyPem: string): Promise<string | null> {
  try {
    // First, check if the key is double base64 encoded
    // If it starts with LS0t (base64 for "---"), it's double encoded
    let actualKey = privateKeyPem
    if (privateKeyPem.startsWith('LS0t')) {
      // Decode the first layer of base64 to get the PEM
      actualKey = atob(privateKeyPem)
    }
    
    // Convert private key - handle both base64 and PEM formats
    let keyBuffer: ArrayBuffer
    
    // Check if it's PEM format (has headers)
    if (actualKey.includes('-----BEGIN')) {
      // It's PEM format, extract base64
      keyBuffer = pemToArrayBuffer(actualKey)
    } else {
      // It's pure base64, decode directly
      const binaryString = atob(actualKey)
      const bytes = new Uint8Array(binaryString.length)
      for (let i = 0; i < binaryString.length; i++) {
        bytes[i] = binaryString.charCodeAt(i)
      }
      keyBuffer = bytes.buffer
    }
    
    // Import the private key
    const privateKey = await crypto.subtle.importKey(
      'pkcs8',
      keyBuffer,
      {
        name: 'RSA-OAEP',
        hash: 'SHA-256',
      },
      false,
      ['decrypt']
    )

    // Decode base64 to array buffer
    const encryptedData = base64ToArrayBuffer(encryptedBase64)

    // Decrypt
    const decryptedBuffer = await crypto.subtle.decrypt(
      {
        name: 'RSA-OAEP',
      },
      privateKey,
      encryptedData
    )

    // Convert to string
    return new TextDecoder().decode(decryptedBuffer)
  } catch (error) {
    // Silently fail - no logging in production
    return null
  }
}

function pemToArrayBuffer(pem: string): ArrayBuffer {
  // Handle both base64 encoded and PEM format
  let b64 = pem
  
  // If it's in PEM format, extract the base64 content
  if (pem.includes('-----BEGIN')) {
    b64 = pem
      .replace(/-----BEGIN PRIVATE KEY-----/g, '')
      .replace(/-----END PRIVATE KEY-----/g, '')
      .replace(/-----BEGIN RSA PRIVATE KEY-----/g, '')
      .replace(/-----END RSA PRIVATE KEY-----/g, '')
      .replace(/\s/g, '')
  }
  
  const binaryString = atob(b64)
  const bytes = new Uint8Array(binaryString.length)
  for (let i = 0; i < binaryString.length; i++) {
    bytes[i] = binaryString.charCodeAt(i)
  }
  return bytes.buffer
}

function base64ToArrayBuffer(base64: string): ArrayBuffer {
  const binaryString = atob(base64)
  const bytes = new Uint8Array(binaryString.length)
  for (let i = 0; i < binaryString.length; i++) {
    bytes[i] = binaryString.charCodeAt(i)
  }
  return bytes.buffer
}

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    // Validate request method
    if (req.method !== 'POST') {
      return new Response(
        JSON.stringify({ error: 'Method not allowed' }),
        { status: 405, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Parse request body
    const body: SubmissionRequest = await req.json()

    // Validate required fields
    if (!body.smsBody || !body.sender || !body.encryptedDeviceData) {
      return new Response(
        JSON.stringify({ error: 'Missing required fields' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Get private key from environment
    const privateKey = Deno.env.get('APP_PRIVATE_KEY')
    if (!privateKey) {
      return new Response(
        JSON.stringify({ error: 'Server configuration error' }),
        { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Decrypt and verify device data
    const decryptedData = await decryptWithPrivateKey(body.encryptedDeviceData, privateKey)
    if (!decryptedData) {
      return new Response(
        JSON.stringify({ error: 'Invalid device verification' }),
        { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Parse decrypted data (format: "deviceId|timestamp")
    const [deviceId, timestampStr] = decryptedData.split('|')
    const timestamp = parseInt(timestampStr)

    // Verify timestamp (must be within last 5 minutes)
    const now = Date.now()
    if (isNaN(timestamp) || now - timestamp > 5 * 60 * 1000) {
      return new Response(
        JSON.stringify({ error: 'Request expired' }),
        { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Create Supabase client
    const supabaseUrl = Deno.env.get('SUPABASE_URL')!
    const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
    const supabase = createClient(supabaseUrl, supabaseServiceKey)

    // Determine submission status
    const isSupported = !!body.parsedData
    const isParseable = isSupported && body.parsedData?.amount !== undefined
    const status = body.userExpected?.isTransaction === false 
      ? 'rejected' 
      : body.mode === 'improve' 
        ? 'improvement' 
        : 'pending'

    // Calculate confidence score
    let confidenceScore = null
    if (isParseable && body.parsedData) {
      const fields = ['amount', 'type', 'merchant', 'category', 'reference', 'accountLast4']
      const filledFields = fields.filter(field => body.parsedData?.[field as keyof typeof body.parsedData] !== undefined).length
      confidenceScore = filledFields / fields.length
    }

    // Insert submission (we don't store deviceId for privacy)
    const { data, error } = await supabase
      .from('submissions')
      .insert({
        sms_body: body.smsBody,
        sender: body.sender,
        is_supported: isSupported,
        is_parseable: isParseable,
        confidence_score: confidenceScore,
        parsed_data: body.parsedData || null,
        user_expected: body.userExpected || null,
        admin_notes: body.additionalNotes || null,
        status: status,
        created_at: new Date().toISOString()
      })
      .select()
      .single()

    if (error) {
      return new Response(
        JSON.stringify({ error: 'Failed to save submission' }),
        { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Return success response
    return new Response(
      JSON.stringify({ 
        success: true, 
        id: data.id,
        message: 'Thank you for your submission.'
      }),
      { 
        status: 200, 
        headers: { ...corsHeaders, 'Content-Type': 'application/json' } 
      }
    )

  } catch (error) {
    return new Response(
      JSON.stringify({ error: 'Internal server error' }),
      { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }
})
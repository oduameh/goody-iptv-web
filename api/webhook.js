// api/webhook.js - Vercel serverless function for Stripe webhooks
import { createHash } from 'crypto';
import Stripe from 'stripe';
import { storeCompletedPayment } from './check-payment.js';

// Initialize Stripe with secret key from environment
const stripe = new Stripe(process.env.STRIPE_SECRET_KEY, {
  apiVersion: '2023-10-16',
});

// Simple license generation
function generateLicenseKey(deviceId, timestamp = Date.now()) {
  const data = `${deviceId}-${timestamp}-GOODY2024`;
  return createHash('sha256').update(data).digest('hex').slice(0, 16).toUpperCase();
}

// Simple email sending with Resend
async function sendLicenseEmail(email, licenseKey, customerName = '') {
  // Log for debugging
  console.log(`📧 Sending license email to: ${email}`);
  console.log(`🔑 License Key: ${licenseKey}`);
  console.log(`👤 Customer: ${customerName}`);
  
  // Send email with Resend if API key is configured
  if (process.env.RESEND_API_KEY) {
    try {
      const response = await fetch('https://api.resend.com/emails', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${process.env.RESEND_API_KEY}`,
          'Content-Type': 'application/json',
        },
            body: JSON.stringify({
          from: process.env.FROM_EMAIL || 'Goody IPTV <noreply@goodytv.com>',
          to: [email],
          subject: '🎉 Your Goody IPTV Premium License',
          html: `
            <h1>Welcome to Goody IPTV Premium!</h1>
            <p>Hi ${customerName || 'there'},</p>
            <p>Thank you for purchasing Goody IPTV Premium! Your license is ready.</p>
            <div style="background:#f8f9fa; border:2px dashed #6c757d; padding:20px; margin:20px 0; text-align:center;">
              <h2>Your License Key</h2>
              <code style="font-size:24px; letter-spacing:3px; color:#007bff;">${licenseKey}</code>
            </div>
            <h3>How to activate:</h3>
            <ol>
              <li>Open Goody IPTV on any device</li>
              <li>When the trial expires, enter this license key</li>
              <li>Enjoy 180 days of premium features!</li>
            </ol>
            <p><strong>Premium Features Unlocked:</strong></p>
            <ul>
              <li>✅ Advanced 7-day EPG</li>
              <li>✅ Watch history & resume</li>
              <li>✅ Multiple themes</li>
              <li>✅ Grid/List/Compact views</li>
              <li>✅ Multiple playlists</li>
              <li>✅ Works on all your devices</li>
            </ul>
            <p>Valid until: ${new Date(Date.now() + 180 * 24 * 60 * 60 * 1000).toLocaleDateString()}</p>
            <hr>
            <p><small>Need help? Contact support@goodytv.com</small></p>
          `
        })
      });
      
      if (!response.ok) {
        throw new Error(`Email failed: ${response.status}`);
      }
      
      console.log('✅ License email sent successfully');
    } catch (error) {
      console.error('❌ Failed to send email:', error);
      // Don't throw - license generation should still succeed
    }
  } else {
    console.log('⚠️ No RESEND_API_KEY configured - email not sent');
  }
}

// Store license (replace with your database)
async function storeLicense(deviceId, licenseKey, email, sessionId) {
  // TODO: Replace with actual database (Supabase, Firebase, etc.)
  console.log(`💾 Storing license:`, {
    deviceId,
    licenseKey,
    email,
    sessionId,
    expiresAt: new Date(Date.now() + 180 * 24 * 60 * 60 * 1000),
    createdAt: new Date()
  });
  
  // Example with Supabase (uncomment when you have it set up)
  /*
  const { createClient } = require('@supabase/supabase-js');
  const supabase = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_KEY);
  
  const { error } = await supabase
    .from('licenses')
    .insert({
      device_id: deviceId,
      license_key: licenseKey,
      customer_email: email,
      stripe_session_id: sessionId,
      expires_at: new Date(Date.now() + 180 * 24 * 60 * 60 * 1000),
      created_at: new Date()
    });
    
  if (error) throw error;
  */
}

export default async function handler(req, res) {
  // Only allow POST requests
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    // Verify Stripe webhook signature for security
    const sig = req.headers['stripe-signature'];
    const rawBody = JSON.stringify(req.body);
    
    let event;
    
    if (process.env.STRIPE_WEBHOOK_SECRET && sig) {
      // Production: verify webhook signature
      try {
        event = stripe.webhooks.constructEvent(rawBody, sig, process.env.STRIPE_WEBHOOK_SECRET);
        console.log('✅ Webhook signature verified');
      } catch (err) {
        console.error('❌ Webhook signature verification failed:', err.message);
        return res.status(400).send(`Webhook Error: ${err.message}`);
      }
    } else {
      // Development: accept webhook without verification (NOT for production)
      console.log('⚠️ WARNING: Webhook signature not verified (development mode)');
      event = req.body;
    }

    console.log(`📦 Webhook received:`, event.type);

    // Handle successful payment
    if (event.type === 'checkout.session.completed') {
      const session = event.data.object;
      
      // Extract customer info
      const deviceId = session.client_reference_id || `device_${Date.now()}`;
      const email = session.customer_details?.email || session.customer_email;
      const customerName = session.customer_details?.name || '';
      
      console.log(`💳 Payment completed for device: ${deviceId}`);
      
      // Generate license key
      const licenseKey = generateLicenseKey(deviceId);
      
                // Store license in database
          await storeLicense(deviceId, licenseKey, email, session.id);

          // Store completed payment for instant checking
          storeCompletedPayment(deviceId, licenseKey);

          // Send license key to customer
          await sendLicenseEmail(email, licenseKey, customerName);

          console.log(`✅ License ${licenseKey} generated and sent to ${email}`);
    }

    // Respond to Stripe
    res.status(200).json({ received: true });
    
  } catch (error) {
    console.error('❌ Webhook error:', error);
    res.status(400).json({ error: `Webhook Error: ${error.message}` });
  }
}

// For testing: POST to /api/webhook with mock data
export const config = {
  api: {
    bodyParser: {
      sizeLimit: '1mb',
    },
  },
} 
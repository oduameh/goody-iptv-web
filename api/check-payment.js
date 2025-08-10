// api/check-payment.js - Check if payment was completed for a device
import { createHash } from 'crypto';

// Simple in-memory storage for completed payments (replace with database in production)
const completedPayments = new Map();

// Generate license key (same logic as webhook)
function generateLicenseKey(deviceId, timestamp = Date.now()) {
  const data = `${deviceId}-${timestamp}-GOODY2024`;
  return createHash('sha256').update(data).digest('hex').slice(0, 16).toUpperCase();
}

export default async function handler(req, res) {
  // Enable CORS for frontend requests
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  if (req.method !== 'GET') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    const { deviceId } = req.query;

    if (!deviceId) {
      return res.status(400).json({ error: 'Device ID is required' });
    }

    console.log(`🔍 Checking payment for device: ${deviceId}`);

    // Check if payment was completed for this device
    const paymentData = completedPayments.get(deviceId);

    if (paymentData) {
      console.log(`✅ Payment found for device: ${deviceId}`);
      return res.status(200).json({
        paid: true,
        licenseKey: paymentData.licenseKey,
        timestamp: paymentData.timestamp
      });
    }

    console.log(`❌ No payment found for device: ${deviceId}`);
    return res.status(200).json({
      paid: false
    });

  } catch (error) {
    console.error('❌ Payment check error:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

// Export function to store completed payments (called by webhook)
export function storeCompletedPayment(deviceId, licenseKey) {
  completedPayments.set(deviceId, {
    licenseKey,
    timestamp: Date.now()
  });
  console.log(`💾 Stored payment for device: ${deviceId}`);
} 
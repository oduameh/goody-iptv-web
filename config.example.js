// config.js - Client-side configuration for Goody IPTV
// Copy this file to config.js and fill in your actual values
// DO NOT commit config.js to git - it contains your API keys

window.GOODY_CONFIG = {
  // =============================================================================
  // STRIPE CONFIGURATION
  // =============================================================================
  
  // Stripe Publishable Key (safe to expose in frontend)
  // Get from: https://dashboard.stripe.com/apikeys
  STRIPE_PUBLIC_KEY: 'pk_test_your_publishable_key_here',
  
  // Stripe Payment Link URL
  // Create at: https://dashboard.stripe.com/payment-links
  // Product: "Goody IPTV Premium License" - $4.99
  STRIPE_PAYMENT_LINK: 'https://buy.stripe.com/YOUR_PAYMENT_LINK',
  
  // =============================================================================
  // APP CONFIGURATION
  // =============================================================================
  
  // Your domain (for analytics and tracking)
  APP_DOMAIN: 'https://yourdomain.com',
  
  // Support email
  SUPPORT_EMAIL: 'support@yourdomain.com',
  
  // License purchase email  
  LICENSE_EMAIL: 'license@yourdomain.com',
  
  // =============================================================================
  // ANALYTICS (Optional)
  // =============================================================================
  
  // Google Analytics Measurement ID
  GA_MEASUREMENT_ID: 'G-XXXXXXXXXX',
  
  // =============================================================================
  // ENVIRONMENT DETECTION
  // =============================================================================
  
  // Automatically detect environment
  IS_DEVELOPMENT: window.location.hostname === 'localhost' || 
                  window.location.hostname === '127.0.0.1' ||
                  window.location.hostname.includes('github.io'),
};

// Helper functions to access config safely
window.getConfig = function(key, defaultValue = null) {
  return window.GOODY_CONFIG?.[key] || defaultValue;
};

// Convenience aliases for common values
window.STRIPE_PUBLIC_KEY = window.getConfig('STRIPE_PUBLIC_KEY');
window.STRIPE_PAYMENT_LINK = window.getConfig('STRIPE_PAYMENT_LINK');

// Development warning
if (window.getConfig('IS_DEVELOPMENT')) {
  console.log('🚧 Goody IPTV running in development mode');
  console.log('📝 Configure your API keys in config.js for production');
} 